package net.joth.grabby.networking

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis
import dev.ryanhcode.sable.api.physics.constraint.free.FreeConstraintConfiguration
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer
import dev.ryanhcode.sable.companion.SableCompanion
import dev.ryanhcode.sable.companion.math.BoundingBox3i
import dev.ryanhcode.sable.sublevel.ServerSubLevel
import net.joth.grabby.physics.GrabData
import net.joth.grabby.Grabby
import net.joth.grabby.GrabbyConfig
import net.joth.grabby.physics.AlignmentData
import net.joth.grabby.physics.GrabbyAssemblyHelper
import net.joth.grabby.physics.GrabbyState
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.phys.Vec3
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import org.joml.Quaterniond
import org.joml.Vector3d
import java.util.UUID
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.roundToInt

private const val LINEAR_STIFFNESS = 1000.0
private const val LINEAR_DAMPING = 50.0
private const val ANGULAR_STIFFNESS = 13000.0
private const val ANGULAR_DAMPING = 1000.0


object GrabbyNetworking {

    @SubscribeEvent
    fun onRegisterPayloads(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("1")
        registrar.playToServer(GrabAssemblePacket.TYPE, GrabAssemblePacket.STREAM_CODEC, ::handleAssemble)
        registrar.playToServer(GrabSubLevelPacket.TYPE, GrabSubLevelPacket.STREAM_CODEC, ::handleGrabSubLevel)
        registrar.playToServer(GrabReleasePacket.TYPE, GrabReleasePacket.STREAM_CODEC, ::handleRelease)
        registrar.playToServer(DisassemblePacket.TYPE, DisassemblePacket.STREAM_CODEC, ::handleDisassemble)
    }

    private fun handleAssemble(packet: GrabAssemblePacket, context: IPayloadContext) {
        // enqueueing work. fs it says on the function. the network thread queues up work on the main thread to be done as soon as it's able
        context.enqueueWork {
            val player = context.player()
            val level = player.level() as? ServerLevel ?: return@enqueueWork // as? returns NULL on a fail, ?: return@enqueueWork returns *from* enqueueWork
            val pos = packet.pos

            if (level.getBlockState(pos).isAir) return@enqueueWork
            if (!player.mainHandItem.isEmpty) return@enqueueWork

            val blocks = GrabbyAssemblyHelper.gatherConnectedBlocks(level, pos)
            val bounds = BoundingBox3i.from(blocks)
            val anchor = blocks.first()
            val subLevel = SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks, bounds)
            Grabby.LOGGER.info("Assembled sub-level ${subLevel?.uniqueId} from ${blocks.size} block(s) at $pos")
            if (subLevel != null) {
                val subLevelAccess = subLevel as dev.ryanhcode.sable.companion.SubLevelAccess

                val plotSpaceGrabbedBlock = subLevel.plot.centerBlock
                val localHitOffset = packet.hitLocation.subtract(Vec3.atCenterOf(pos))
                val grabPointLocal = Vec3.atCenterOf(plotSpaceGrabbedBlock).add(localHitOffset)

                val comPos = subLevelAccess.logicalPose().position()
                val approxTopSurface = Vec3(comPos.x(), comPos.y() + 0.5, comPos.z())
                val grabDistance = minOf(player.getEyePosition().distanceTo(approxTopSurface), 2.5)

                GrabbyState.setHeld(player.uuid, GrabData(subLevel, subLevelAccess, grabPointLocal, grabDistance))
                Grabby.LOGGER.info("Assembled and auto-grabbed sub-level ${subLevel.uniqueId}")
            }
        }
    }

    private fun handleGrabSubLevel(packet: GrabSubLevelPacket, context: IPayloadContext) {
        Grabby.LOGGER.info("grab: handler invoked, pos = ${packet.pos}")
        context.enqueueWork {
            Grabby.LOGGER.info("grab: enqueueWork running")
            val player = context.player()
            val level = player.level() as? ServerLevel ?: run {
                Grabby.LOGGER.info("grab: failed level cast")
                return@enqueueWork
            }
            val pos = packet.pos
            Grabby.LOGGER.info("grab: received pos ${packet.pos}")

            if (!player.mainHandItem.isEmpty) {
                Grabby.LOGGER.info("grab: rejected - hand not empty")
                return@enqueueWork
            }
            if (GrabbyState.getHeld(player.uuid) != null) {
                Grabby.LOGGER.info("grab: rejected - already holding")
                return@enqueueWork
            }

            val subLevelAccess = SableCompanion.INSTANCE.getContaining(level, pos) ?: return@enqueueWork
            Grabby.LOGGER.info("grab: subLevelAccess = $subLevelAccess")
            val subLevel = subLevelAccess as? ServerSubLevel
            Grabby.LOGGER.info("grab: subLevel cast = $subLevel")
            if (subLevel == null) return@enqueueWork


            val grabPointLocal = packet.hitLocation
            //val worldPos = subLevelAccess.logicalPose().transformPosition(grabPointLocal)
            val comPos = subLevelAccess.logicalPose().position()
            //val comWorld = net.minecraft.world.phys.Vec3(comPos.x(), comPos.y(), comPos.z())
            val approxTopSurface = Vec3(comPos.x(), comPos.y() + 0.5, comPos.z())
            val grabDistance = minOf(player.getEyePosition().distanceTo(approxTopSurface), 2.5)

            GrabbyState.setHeld(player.uuid, GrabData(subLevel, subLevelAccess, grabPointLocal, grabDistance))
            Grabby.LOGGER.info("grab: success, stored GrabData")
            Grabby.LOGGER.info("Player ${player.name.string} grabbed sub-level ${subLevel.uniqueId}")
        }
    }
    private fun handleRelease(packet: GrabReleasePacket, context: IPayloadContext) {
        context.enqueueWork {
            GrabbyState.clearHeld(context.player().uuid)
        }
    }
    private fun handleDisassemble(packet: DisassemblePacket, context: IPayloadContext) {
        context.enqueueWork {
            val player = context.player()
            val level = player.level() as? ServerLevel ?: return@enqueueWork
            val grabData = GrabbyState.getHeld(player.uuid) ?: return@enqueueWork
            val subLevel = grabData.subLevel

            // Size check
            val blocks = collectSubLevelBlocks(subLevel)
            val maxSize = GrabbyConfig.maxDisassemblySize.get()
            if (blocks.size > maxSize) {
                player.sendSystemMessage(Component.literal(
                    "Too large to disassemble — ${blocks.size} blocks (max $maxSize)"
                ))
                return@enqueueWork
            }

            val container = SubLevelContainer.getContainer(level) ?: return@enqueueWork
            val pipeline = container.physicsSystem().getPipeline()

            val pose = grabData.subLevelAccess.logicalPose()
            val q = pose.orientation()
            val yawRad = atan2(
                2.0 * (q.w() * q.y() + q.z() * q.x()),
                1.0 - 2.0 * (q.x() * q.x() + q.y() * q.y())
            )
            val ninety = Math.PI / 2.0
            val turns = -floor(yawRad / ninety + 0.5).toInt()
            val disassemblyOrientation = Quaterniond().rotateY(turns * ninety)

            val comLocal = Vector3d(
                subLevel.plot.centerBlock.x + 0.5,
                subLevel.plot.centerBlock.y + 0.5,
                subLevel.plot.centerBlock.z + 0.5
            )

            val anchorLocal = Vector3d(
                subLevel.plot.centerBlock.x + 0.5,
                subLevel.plot.centerBlock.y + 0.5,
                subLevel.plot.centerBlock.z + 0.5
            )

            val comToAnchorOffset = Vector3d(anchorLocal).sub(comLocal)

            GrabbyState.clearHeld(player.uuid)

            val config = FreeConstraintConfiguration(
                Vector3d(),
                anchorLocal,
                disassemblyOrientation
            )
            val alignmentConstraint = pipeline.addConstraint(null, subLevel, config)

            alignmentConstraint.setMotor(ConstraintJointAxis.ANGULAR_X, 0.0, ANGULAR_STIFFNESS, ANGULAR_DAMPING, false, 0.0)
            alignmentConstraint.setMotor(ConstraintJointAxis.ANGULAR_Y, 0.0, ANGULAR_STIFFNESS, ANGULAR_DAMPING, false, 0.0)
            alignmentConstraint.setMotor(ConstraintJointAxis.ANGULAR_Z, 0.0, ANGULAR_STIFFNESS, ANGULAR_DAMPING, false, 0.0)

            // Linear: near-zero stiffness — stop drift, don't force position yet
            alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_X, 0.0, 0.000001, LINEAR_DAMPING, false, 0.0)
            alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Y, 0.0, 0.000001, LINEAR_DAMPING, false, 0.0)
            alignmentConstraint.setMotor(ConstraintJointAxis.LINEAR_Z, 0.0, 0.000001, LINEAR_DAMPING, false, 0.0)

            GrabbyState.setAligning(player.uuid, AlignmentData(
                subLevel, grabData.subLevelAccess,
                alignmentConstraint, disassemblyOrientation, turns,
                blocks
            )
            )
        }
    }

    internal fun executeDisassembly(playerUUID: UUID, data: AlignmentData, level: ServerLevel, player: ServerPlayer) {
        GrabbyState.clearAligning(playerUUID)

        val pose = data.subLevelAccess.logicalPose()
        val comWorld = pose.position()

        val anchorCenter = Vec3.atCenterOf(data.subLevel.plot.centerBlock)
        val anchorWorld = pose.transformPosition(anchorCenter)

        val subLevelAnchor = data.subLevel.plot.centerBlock
        val goal = BlockPos.containing(anchorWorld)
        val rotation = rotationFromTurns(data.disassemblyAngle)
        val quarterTurns = if (rotation == Rotation.NONE) 0 else (4 - rotation.ordinal)

        val plotBounds = BoundingBox3i(data.subLevel.plot.boundingBox)
        val transform = SubLevelAssemblyHelper.AssemblyTransform(
            subLevelAnchor, goal, quarterTurns, rotation, level
        )

        val blocks = mutableListOf<BlockPos>()
        for (chunk in data.subLevel.plot.loadedChunks) {
            val chunkBounds = chunk.boundingBox ?: continue
            for (x in chunkBounds.minX()..chunkBounds.maxX())
                for (y in chunkBounds.minY()..chunkBounds.maxY())
                    for (z in chunkBounds.minZ()..chunkBounds.maxZ()) {
                        val pos = BlockPos(x + chunk.pos.minBlockX, y, z + chunk.pos.minBlockZ)
                        if (!level.getBlockState(pos).isAir) blocks.add(pos)
                    }
        }

        for (block in blocks) {
            if (!level.getBlockState(transform.apply(block)).isAir) {
                player.sendSystemMessage(Component.literal("No space to place the block"))
                return
            }
        }

        if (blocks.isNotEmpty()) {
            SubLevelAssemblyHelper.moveBlocks(level, transform, blocks)
        }
        SubLevelAssemblyHelper.moveTrackingPoints(level, plotBounds, null, transform)

        level.playSound(null, goal, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f)
    }

    private fun rotationFromTurns(turns: Int): Rotation =
        when (Math.floorMod(turns, 4)) {
            0 -> Rotation.NONE
            1 -> Rotation.COUNTERCLOCKWISE_90
            2 -> Rotation.CLOCKWISE_180
            3 -> Rotation.CLOCKWISE_90
            else -> Rotation.NONE
        }

    private fun collectSubLevelBlocks(subLevel: ServerSubLevel): List<BlockPos> {
        val blocks = mutableListOf<BlockPos>()
        val plot = subLevel.plot
        for (chunk in plot.loadedChunks) {
            val bounds = chunk.boundingBox ?: continue
            for (x in bounds.minX()..bounds.maxX())
                for (y in bounds.minY()..bounds.maxY())
                    for (z in bounds.minZ()..bounds.maxZ()) {
                        val pos = BlockPos(x + chunk.pos.minBlockX, y, z + chunk.pos.minBlockZ)
                        if (!subLevel.level.getBlockState(pos).isAir)
                            blocks.add(pos)
                    }
        }
        return blocks
    }

    private data class AlignmentResult(
        val isAligned: Boolean,
        val nearestYawDeg: Double
    )
}

