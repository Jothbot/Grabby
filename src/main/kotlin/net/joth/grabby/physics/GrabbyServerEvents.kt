package net.joth.grabby.physics

import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis
import dev.ryanhcode.sable.api.physics.constraint.free.FreeConstraintConfiguration
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer
import dev.ryanhcode.sable.Sable
import net.joth.grabby.Grabby
import net.joth.grabby.GrabbyConfig
import net.joth.grabby.networking.GrabbyNetworking
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.living.LivingEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.server.ServerLifecycleHooks
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.floor

private const val READY_TICKS_REQUIRED = 5

object GrabbyServerEvents {
    private const val LINEAR_STIFFNESS = 1000.0
    private const val LINEAR_DAMPING = 50.0

    private const val MOVING_ANGULAR_STIFFNESS = 400.0
    private const val MOVING_ANGULAR_DAMPING = 40.0

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) { //ServerTickEvent -> every tick, Post -> after all the logic has processed that tick
        val server = ServerLifecycleHooks.getCurrentServer() ?: return  //Just regular old "stop running this when something stupid like "server code run without server" is happening"

        for ((level, positions) in GrabbyState.drainNeighborUpdates()) {
            for (pos in positions) {
                val stateAtPos = level.getBlockState(pos)
                for (dir in Direction.entries) {
                    val neighborPos = pos.relative(dir)
                    val neighborState = level.getBlockState(neighborPos)
                    val updatedState = neighborState.updateShape(dir.opposite, stateAtPos, level, neighborPos, pos)
                    if (updatedState != neighborState) {
                        level.setBlock(neighborPos, updatedState, Block.UPDATE_ALL)
                    }
                }
            }
        }

        val springConstant = GrabbyConfig.springConstant.get()
        val dampingConstant = GrabbyConfig.dampingConstant.get()
        val angularDamping = GrabbyConfig.angularDamping.get()
        val maxForce = GrabbyConfig.maxForce.get()

        for ((playerUUID, grabData) in GrabbyState.getAllHeld()) {
            val subLevel = grabData.subLevel

            if (subLevel.isRemoved) {
                GrabbyState.clearHeld(playerUUID)
                continue
            }

            val player = server.playerList.getPlayer(playerUUID) as? ServerPlayer
            if (player == null) {
                GrabbyState.clearHeld(playerUUID)
                continue
            }

            // this stop the player from exploiting troll physics
            val standingOn = Sable.HELPER.getTrackingSubLevel(player)
            if (standingOn != null && standingOn == subLevel) {
                grabData.constraintHandle?.remove()
                grabData.constraintHandle = null
                continue
            }


            val level = player.level() as? ServerLevel ?: continue //get the level
            val container = SubLevelContainer.getContainer(level) ?: continue //get the sublevel container, whatever that is. TODO: FIGURE THIS SHIT OUT
            val pipeline = container.physicsSystem().getPipeline() //physics stuff. i guess? returns the physics container that as i understand actually handles all the physics

            val pose = grabData.subLevelAccess.logicalPose()
            val comPos = pose.position()


            val eye = player.getEyePosition()
            val look = player.lookAngle
            val computedGoal = Vector3d(
                eye.x + look.x * grabData.grabDistance,
                eye.y + look.y * grabData.grabDistance,
                eye.z + look.z * grabData.grabDistance
            )

            if (player.onGround()) {
                grabData.goalFreezeTicksRemaining = 0
            }

            val constraintGoal = if (grabData.goalFreezeTicksRemaining > 0) {
                grabData.goalFreezeTicksRemaining--
                grabData.frozenGoal ?: computedGoal
            } else {
                computedGoal
            }

            val constraintPosition = Vector3d(
                grabData.grabPointLocal.x,
                grabData.grabPointLocal.y,
                grabData.grabPointLocal.z
            )

            grabData.constraintHandle?.remove()

            val targetOrientation = grabData.targetOrientation ?: Quaterniond()
            val constraint = pipeline.addConstraint(
                null, subLevel,
                FreeConstraintConfiguration(constraintGoal, constraintPosition, targetOrientation)
            )

            for (axis in ConstraintJointAxis.LINEAR) {
                constraint.setMotor(axis, 0.0, springConstant, dampingConstant, true, maxForce)
            }

            if (grabData.targetOrientation != null) {
                // grab with the moving item, dampened rotation
                for (axis in ConstraintJointAxis.ANGULAR) {
                    constraint.setMotor(axis, 0.0, MOVING_ANGULAR_STIFFNESS, MOVING_ANGULAR_DAMPING, true, maxForce)
                }
            } else {
                // nomral grab
                for (axis in ConstraintJointAxis.ANGULAR) {
                    constraint.setMotor(axis, 0.0, 0.0, angularDamping, true, maxForce)
                }
            }

            grabData.constraintHandle = constraint
        }
        for ((playerUUID, alignmentData) in GrabbyState.getAllAligning()) {
            alignmentData.aligningTicks++

            if (alignmentData.aligningTicks > GrabbyConfig.alignmentMaxTicks.get()) {
                GrabbyState.clearAligning(playerUUID)
                val player = server.playerList.getPlayer(playerUUID) as? ServerPlayer
                player?.sendSystemMessage(Component.literal("Could not align — try again in a less obstructed area"))
                continue
            }

            val player = server.playerList.getPlayer(playerUUID) as? ServerPlayer
            if (player == null) {
                GrabbyState.clearAligning(playerUUID)
                continue
            }

            val level = player.level() as? ServerLevel ?: continue
            val pose = alignmentData.subLevelAccess.logicalPose()


            // This should fix the issue of the sublevel flying off
            // before, it would snap the center of mass to the goal
            // not it uses the anchor for that
            val anchorCenter = Vec3.atCenterOf(alignmentData.subLevel.plot.centerBlock)
            val anchorWorld = pose.transformPosition(anchorCenter)
            val snapped = Vector3d(
                floor(anchorWorld.x) + 0.5,
                floor(anchorWorld.y) + 0.5,
                floor(anchorWorld.z) + 0.5
            )
            val localGoal = alignmentData.disassemblyOrientation.transformInverse(snapped, Vector3d())

            alignmentData.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_X, localGoal.x, LINEAR_STIFFNESS, LINEAR_DAMPING, false, 0.0)
            alignmentData.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Y, localGoal.y, LINEAR_STIFFNESS, LINEAR_DAMPING, false, 0.0)
            alignmentData.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Z, localGoal.z, LINEAR_STIFFNESS, LINEAR_DAMPING, false, 0.0)

            val rawAngle = pose.orientation().div(alignmentData.disassemblyOrientation, Quaterniond()).angle()
            val angle = minOf(rawAngle, 2 * Math.PI - rawAngle)
            val positionError = Vector3d(anchorWorld.x, anchorWorld.y, anchorWorld.z).distance(snapped)

            val angleDeg = Math.toDegrees(Math.abs(angle))
            val rotTolerance = GrabbyConfig.disassemblyRotationTolerance.get()
            val posTolerance = GrabbyConfig.disassemblyPositionTolerance.get()

            if (angleDeg <= rotTolerance && positionError < posTolerance) {
                alignmentData.readyTicks++
            } else {
                if (alignmentData.aligningTicks % 20 == 0) {
                    Grabby.LOGGER.info(
                        "Alignment tick ${alignmentData.aligningTicks}: " +
                        "angle=${String.format("%.2f", angleDeg)}° (need <${rotTolerance}°) " +
                        "posErr=${String.format("%.3f", positionError)} (need <${posTolerance})"
                    )
                }
                alignmentData.readyTicks = 0
            }

            if (alignmentData.readyTicks >= READY_TICKS_REQUIRED) {
                GrabbyNetworking.executeDisassembly(playerUUID, alignmentData, level, player)
            }
        }
    }

    // another anti-troll physics solution
    @SubscribeEvent
    fun onPlayerJump(event: LivingEvent.LivingJumpEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (player.onClimbable()) return
        val grabData = GrabbyState.getHeld(player.uuid) ?: return
        val eye = player.getEyePosition()
        val look = player.lookAngle
        grabData.frozenGoal = Vector3d(
            eye.x + look.x * grabData.grabDistance,
            eye.y + look.y * grabData.grabDistance,
            eye.z + look.z * grabData.grabDistance
        )
        grabData.goalFreezeTicksRemaining = 20
    }
}