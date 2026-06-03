package net.joth.grabby.items

import net.joth.grabby.Grabby
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.Item
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier


class MovingItem : Item(
    Properties()
        .stacksTo(1)
)


object GrabbyItems {
    private val ITEMS: DeferredRegister<Item> = DeferredRegister.create(Registries.ITEM, Grabby.MOD_ID)

    val MOVING_ITEM = ITEMS.register("moving_item", Supplier { MovingItem() })

    fun register(modBus: IEventBus) = ITEMS.register(modBus)
}
