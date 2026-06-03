package net.joth.grabby

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block

object GrabbyTags {
    val BLACKLIST_ASSEMBLY: TagKey<Block> = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(Grabby.MOD_ID, "blacklist_assembly")
    )
}
