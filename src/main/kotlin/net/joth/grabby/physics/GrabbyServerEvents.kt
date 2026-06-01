package net.joth.grabby.physics

import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis
import dev.ryanhcode.sable.api.physics.constraint.free.FreeConstraintConfiguration
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer
import dev.ryanhcode.sable.Sable
import net.joth.grabby.GrabbyConfig
import net.joth.grabby.networking.GrabbyNetworking
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.server.ServerLifecycleHooks
import org.joml.Quaterniond
import org.joml.Vector3d

private const val READY_TICKS_REQUIRED = 5

object GrabbyServerEvents {
    private const val LINEAR_STIFFNESS = 1000.0
    private const val LINEAR_DAMPING = 50.0

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) { //ServerTickEvent -> every tick, Post -> after all the logic has processed that tick
        val server = ServerLifecycleHooks.getCurrentServer() ?: return  //Just regular old "stop running this when something stupid like "server code run without server" is happening"

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
            val grabPointWorld = Vector3d(comPos.x(), comPos.y(), comPos.z())


            val eye = player.getEyePosition()
            val look = player.lookAngle
            val constraintGoal = Vector3d(
                eye.x + look.x * grabData.grabDistance,
                eye.y + look.y * grabData.grabDistance,
                eye.z + look.z * grabData.grabDistance
            )

            val constraintPosition = Vector3d(
                grabData.grabPointLocal.x,
                grabData.grabPointLocal.y,
                grabData.grabPointLocal.z
            )

            grabData.constraintHandle?.remove()

            val constraint = pipeline.addConstraint(
                null, subLevel,
                FreeConstraintConfiguration(constraintGoal, constraintPosition, Quaterniond())
            )

            for (axis in ConstraintJointAxis.LINEAR) {
                constraint.setMotor(axis, 0.0, springConstant, dampingConstant, true, maxForce)
            }

            for (axis in ConstraintJointAxis.ANGULAR) {
                constraint.setMotor(axis, 0.0, 0.0, angularDamping, true, maxForce)
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


            val comWorld = pose.position()
            val current = Vector3d(
                Math.floor(comWorld.x()) + 0.5,
                Math.floor(comWorld.y()) + 0.5,
                Math.floor(comWorld.z()) + 0.5
            )
            val goal = Vector3d(current).floor().add(0.5, 0.5, 0.5)
            val localGoal = alignmentData.disassemblyOrientation.transformInverse(goal, Vector3d())

            alignmentData.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_X, localGoal.x, LINEAR_STIFFNESS, LINEAR_DAMPING, false, 0.0)
            alignmentData.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Y, localGoal.y, LINEAR_STIFFNESS, LINEAR_DAMPING, false, 0.0)
            alignmentData.alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Z, localGoal.z, LINEAR_STIFFNESS, LINEAR_DAMPING, false, 0.0)

            val angle = pose.orientation().div(alignmentData.disassemblyOrientation, Quaterniond()).angle()
            val positionError = current.distance(goal)

            if (Math.toDegrees(Math.abs(angle)) <= GrabbyConfig.disassemblyRotationTolerance.get()
                && positionError < GrabbyConfig.disassemblyPositionTolerance.get()) {
                alignmentData.readyTicks++
            } else {
                alignmentData.readyTicks = 0
            }

            if (alignmentData.readyTicks >= READY_TICKS_REQUIRED) {
                GrabbyNetworking.executeDisassembly(playerUUID, alignmentData, level, player)
            }
        }
    }
}