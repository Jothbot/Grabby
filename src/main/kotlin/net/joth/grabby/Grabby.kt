package net.joth.grabby

import net.joth.grabby.client.GrabbyClientEvents
import net.joth.grabby.client.GrabbyKeybinds
import net.joth.grabby.compat.AccessoriesCompat
import net.joth.grabby.items.GrabbyItems
import net.joth.grabby.networking.GrabbyNetworking
import net.joth.grabby.physics.GrabbyServerEvents
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import net.neoforged.neoforge.common.NeoForge
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(Grabby.MOD_ID)
class Grabby(modBus: IEventBus, dist: Dist, modContainer: ModContainer) {

    init {
        LOGGER.log(Level.INFO, "Grabbing everything that isn't nailed down!")

        modBus.addListener(::onCommonSetup)
        modBus.register(GrabbyNetworking)
        GrabbyItems.register(modBus)

        modContainer.registerConfig(ModConfig.Type.SERVER, GrabbyConfig.SPEC)
        NeoForge.EVENT_BUS.register(GrabbyServerEvents)

        // okay so biiiig note on this. I really wanted to use Kotlin for this project, and it has @EventBusSubscriber
        // which in theory should replace all this initialization stuff. So one problem with that, that method is fully
        // broken in the relevant NeoForge versions. So we just won't use it! Hopefully it'll be resolved in the future
        when (dist) {
            Dist.CLIENT -> {
                modBus.addListener(::onClientSetup)
                modBus.register(GrabbyKeybinds)
                NeoForge.EVENT_BUS.register(GrabbyClientEvents)
            }
            Dist.DEDICATED_SERVER -> {
                modBus.addListener(::onServerSetup)
            }
        }
    }

    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        LOGGER.log(Level.INFO, "Grabbing your belongings")
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        LOGGER.log(Level.INFO, "Initializing client...")
        AccessoriesCompat.registerRenderers()
    }

    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
        LOGGER.log(Level.INFO, "Server starting...")
    }

    companion object {  //okay im still figuring this out but companion objects seem to just be for defining stuff for classes??? i guess???
        const val MOD_ID = "grabby"
        @JvmStatic val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    }
}