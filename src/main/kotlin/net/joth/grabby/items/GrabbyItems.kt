package net.joth.grabby.items

import net.joth.grabby.Grabby
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier


class PlumbBobItem : Item(
    Properties()
        .stacksTo(1)
)


object GrabbyItems {
    private val ITEMS: DeferredRegister<Item> = DeferredRegister.create(Registries.ITEM, Grabby.MOD_ID)

    val PLUMB_BOB = ITEMS.register("plumb_bob", Supplier { PlumbBobItem() })

    fun register(modBus: IEventBus) {
        ITEMS.register(modBus)
        modBus.addListener(::onBuildCreativeTab)
    }

    private fun onBuildCreativeTab(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PLUMB_BOB.get())
        }
    }
}
