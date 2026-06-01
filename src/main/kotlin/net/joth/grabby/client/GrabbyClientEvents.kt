package net.joth.grabby.client

import dev.ryanhcode.sable.companion.SableCompanion
import net.joth.grabby.networking.GrabAssemblePacket
import net.joth.grabby.networking.GrabReleasePacket
import net.joth.grabby.networking.GrabSubLevelPacket
import net.joth.grabby.Grabby
import net.joth.grabby.networking.DisassemblePacket
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.network.PacketDistributor

object GrabbyClientEvents {

    var isHolding = false

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        while (GrabbyKeybinds.GRAB_KEY.consumeClick()) {
            val mc = Minecraft.getInstance()
            val player = mc.player ?: return
            val level = mc.level ?: return  // this (?:) notation basically means "if true do the main thing, else (:?) do that"
            val hit = mc.hitResult


            if (player.isCrouching) {
                if (isHolding) {
                    PacketDistributor.sendToServer(DisassemblePacket.INSTANCE)
                    isHolding = false
                } else {
                    // Assemble
                    if (hit is BlockHitResult && hit.type == HitResult.Type.BLOCK) {
                        if (player.mainHandItem.isEmpty) {
                            PacketDistributor.sendToServer(GrabAssemblePacket(hit.blockPos, hit.location))
                            isHolding = true  //TODO: handle this better
                        }
                    }
                }
            } else {
                if (isHolding) {
                    // (Jack Black voice) SUBLEVEL! RELEASE!
                    PacketDistributor.sendToServer(GrabReleasePacket.INSTANCE)
                    isHolding = false
                } else {
                    // Grab onto sublevel
                    if (hit is BlockHitResult && hit.type == HitResult.Type.BLOCK) {
                        val isSublevel = SableCompanion.INSTANCE.isInPlotGrid(level, hit.blockPos)
                        if (isSublevel && player.mainHandItem.isEmpty) {
                            Grabby.LOGGER.info("grab: sending GrabSubLevelPacket")
                            PacketDistributor.sendToServer(GrabSubLevelPacket(hit.blockPos, hit.location))
                            isHolding = true
                        }
                    } else {
                    }
                }
            }
        }
    }
}