package net.joth.grabby.client

import dev.ryanhcode.sable.Sable
import dev.ryanhcode.sable.companion.SableCompanion
import net.joth.grabby.compat.AccessoriesCompat
import net.joth.grabby.items.PlumbBobItem
import net.joth.grabby.networking.DisassemblePacket
import net.joth.grabby.networking.GrabAssemblePacket
import net.joth.grabby.networking.GrabReleasePacket
import net.joth.grabby.networking.GrabSubLevelPacket
import net.joth.grabby.networking.MovingItemDragPacket
import net.joth.grabby.Grabby
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.network.PacketDistributor
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import java.lang.Math.toRadians

object GrabbyClientEvents {

    @JvmField var isHolding = false

    private var movingItemSubLevelId: java.util.UUID? = null
    private var dragOrientation: Quaterniond? = null

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return

        val holdingMovingItem = player.mainHandItem.item is PlumbBobItem
        val hasMovingItem = holdingMovingItem || AccessoriesCompat.isMovingItemEquipped(player)
        val canAct = player.mainHandItem.isEmpty || hasMovingItem

        if (isHolding) player.setSprinting(false)

        GrabbyRotateState.active = hasMovingItem && isHolding && GrabbyKeybinds.ROTATE_KEY.isDown()

        if (GrabbyRotateState.active) {
            val orientation = dragOrientation
            if (orientation != null) {
                val yawRad = toRadians(GrabbyRotateState.pendingYaw)
                val pitchRad = toRadians(GrabbyRotateState.pendingPitch)
                orientation.rotateLocalY(yawRad)
                val look = player.lookAngle
                val right = look.cross(net.minecraft.world.phys.Vec3(0.0, 1.0, 0.0)).normalize()
                orientation.premul(Quaterniond(AxisAngle4d(pitchRad, right.x, right.y, right.z)))
            }
        }
        GrabbyRotateState.pendingYaw = 0.0
        GrabbyRotateState.pendingPitch = 0.0

        val subLevelId = movingItemSubLevelId
        val orientation = dragOrientation
        if (hasMovingItem && isHolding && subLevelId != null && orientation != null) {
            PacketDistributor.sendToServer(MovingItemDragPacket(subLevelId, Quaterniond(orientation)))
        }

        if (isHolding && !canAct) {
            PacketDistributor.sendToServer(GrabReleasePacket.INSTANCE)
            isHolding = false
            clearMovingItemState()
            return
        }

        while (GrabbyKeybinds.GRAB_KEY.consumeClick()) {
            if (!canAct) continue
            val hit = mc.hitResult

            if (player.isCrouching) {
                if (isHolding) {
                    PacketDistributor.sendToServer(DisassemblePacket.INSTANCE)
                    isHolding = false
                    clearMovingItemState()
                } else {
                    if (hit is BlockHitResult && hit.type == HitResult.Type.BLOCK) {
                        PacketDistributor.sendToServer(GrabAssemblePacket(hit.blockPos, hit.location))
                        isHolding = true
                        if (hasMovingItem) initMovingItemState(level, hit)
                    }
                }
            } else {
                if (isHolding) {
                    PacketDistributor.sendToServer(GrabReleasePacket.INSTANCE)
                    isHolding = false
                    clearMovingItemState()
                } else {
                    if (hit is BlockHitResult && hit.type == HitResult.Type.BLOCK) {
                        val isSublevel = SableCompanion.INSTANCE.isInPlotGrid(level, hit.blockPos)
                        if (isSublevel) {
                            Grabby.LOGGER.info("grab: sending GrabSubLevelPacket")
                            PacketDistributor.sendToServer(GrabSubLevelPacket(hit.blockPos, hit.location))
                            isHolding = true
                            if (hasMovingItem) initMovingItemState(level, hit)
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onInteractionKey(event: InputEvent.InteractionKeyMappingTriggered) {
        if (!isHolding) return
        event.isCanceled = true
    }

    private fun initMovingItemState(level: net.minecraft.client.multiplayer.ClientLevel, hit: BlockHitResult) {
        val subLevel = Sable.HELPER.getContainingClient(hit.location)
        movingItemSubLevelId = subLevel?.uniqueId
        dragOrientation = if (subLevel != null) Quaterniond(subLevel.logicalPose().orientation()) else Quaterniond()
    }

    fun onGrabConfirm(subLevelId: java.util.UUID) {
        movingItemSubLevelId = subLevelId
    }

    fun onGrabFailed() {
        isHolding = false
        clearMovingItemState()
    }

    private fun clearMovingItemState() {
        movingItemSubLevelId = null
        dragOrientation = null
        GrabbyRotateState.active = false
    }
}
