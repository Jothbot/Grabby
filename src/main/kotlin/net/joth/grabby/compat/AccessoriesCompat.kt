package net.joth.grabby.compat

import net.minecraft.world.entity.LivingEntity
import net.neoforged.fml.ModList

// this is a safe entrypoint for accessories that we call instead of the main method
object AccessoriesCompat {
    val isLoaded: Boolean by lazy { ModList.get().isLoaded("accessories") }

    fun isMovingItemEquipped(entity: LivingEntity): Boolean {
        if (!isLoaded) return false
        return AccessoriesIntegration.isMovingItemEquipped(entity)
    }
}
