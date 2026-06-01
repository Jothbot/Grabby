package net.joth.grabby.networking

import net.joth.grabby.Grabby
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3


private val VEC3_CODEC: StreamCodec<RegistryFriendlyByteBuf, Vec3> = StreamCodec.of(
    { buf, vec -> buf.writeDouble(vec.x); buf.writeDouble(vec.y); buf.writeDouble(vec.z) },
    { buf -> Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) }
)

@JvmRecord //Just put this before every packet I'm not completely sure what it does but just use it trust me
data class GrabAssemblePacket(val pos: BlockPos, val hitLocation: Vec3) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<GrabAssemblePacket> =
            CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(Grabby.MOD_ID, "grab_assemble"))
            // ResourceLocation.fromNamespaceAndPath() seems like a funny function. I'm not quite sure why I need to use it specifically
            // instead of just making a string using MOD_ID and a path, but I'm not gonna question it. If it's how everyone does it, I'll do it too


        // I'm not even gonna pretend I understand packet stuff. All the networking stuff is just beyond me
        // These 3 lines are the only part that's been straight up vibecoded with Claude because I just could not figure it out
        // Now the mod is AI slop :crying: :puking: :shitting:
        // NOTE: I *did* figure it out. Check the GrabSubLevelPacket.kt for the comment
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, GrabAssemblePacket> =
            StreamCodec.composite(
                BlockPos.STREAM_CODEC, GrabAssemblePacket::pos,
                VEC3_CODEC, GrabAssemblePacket::hitLocation,
                ::GrabAssemblePacket
            )
    }
}

// Unlike all the other packets I've made before, this one is just a regular class, and
// we don't call @JvmRecord (still not sure what that does) TODO: figure out what it does
// because it's not carrying any data
class GrabReleasePacket : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<GrabReleasePacket> =
            CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(Grabby.MOD_ID, "grab_release"))

        // Okay I need your attention on this. DO THIS. Instead of making the game create a new object every time we need
        // to send a packet, just create it once and reuse it. We can do this because it carries no data, so it can be reused.
        // So when calling it in the ClientEvents method, we wouldn't write
        // PacketDistributor.sendToServer(GrabReleasePacket(some data))
        // but instead just
        // PacketDistributor.sendToServer(GrabReleasePacket.INSTANCE)
        val INSTANCE = GrabReleasePacket()

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, GrabReleasePacket> =
            StreamCodec.unit(INSTANCE)
    }
}

// This packet is a data class because it carries data
@JvmRecord
data class GrabSubLevelPacket(val pos: BlockPos, val hitLocation: Vec3) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<GrabSubLevelPacket> =
            CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(Grabby.MOD_ID, "grab_sub_level"))

        // Okay I did some digging and I *think* I get it now. STREAM_CODEC is like a packager that turns the packet into
        // raw data. The getter part is what data we need to get from the packet, the factory is what packet we need to use
        // to reconstruct the data. The first field got me confused for a bit, but I found what it is. It's another codec
        // that we use to encode and decode block position. This is starting to make sense   TODO: Play around with packets, get a better feel for them
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, GrabSubLevelPacket> =
            StreamCodec.composite(
                BlockPos.STREAM_CODEC, GrabSubLevelPacket::pos,
                VEC3_CODEC, GrabSubLevelPacket::hitLocation,
                ::GrabSubLevelPacket
            )
    }
}

class DisassemblePacket : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<DisassemblePacket> =
            CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(Grabby.MOD_ID, "disassemble"))

        val INSTANCE = DisassemblePacket()

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, DisassemblePacket> =
            StreamCodec.unit(INSTANCE)
    }
}