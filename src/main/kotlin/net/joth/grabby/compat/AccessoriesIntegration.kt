package net.joth.grabby.compat

import io.wispforest.accessories.api.AccessoriesCapability
import net.joth.grabby.items.GrabbyItems
import net.minecraft.world.entity.LivingEntity


object AccessoriesIntegration {
    fun isMovingItemEquipped(entity: LivingEntity): Boolean {
        val cap = AccessoriesCapability.get(entity) ?: return false
        return cap.isEquipped(GrabbyItems.MOVING_ITEM.get())
    }
}
