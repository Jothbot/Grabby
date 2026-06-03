package net.joth.grabby.compat

import io.wispforest.accessories.api.AccessoriesCapability
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry
import io.wispforest.accessories.api.client.EmptyRenderer
import net.joth.grabby.items.GrabbyItems
import net.minecraft.world.entity.LivingEntity


object AccessoriesIntegration {
    fun isMovingItemEquipped(entity: LivingEntity): Boolean {
        val cap = AccessoriesCapability.get(entity) ?: return false
        return cap.isEquipped(GrabbyItems.PLUMB_BOB.get())
    }

    fun registerRenderers() {
        AccessoriesRendererRegistry.registerRenderer(GrabbyItems.PLUMB_BOB.get()) { EmptyRenderer() }
    }
}
