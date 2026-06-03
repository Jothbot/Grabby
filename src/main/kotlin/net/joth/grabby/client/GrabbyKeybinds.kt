package net.joth.grabby.client

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import org.lwjgl.glfw.GLFW

object GrabbyKeybinds {
    val GRAB_KEY: KeyMapping = KeyMapping(
        "key.grabby.grab",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_X,
        "key.categories.grabby"
    )

    val ROTATE_KEY: KeyMapping = KeyMapping(
        "key.grabby.rotate",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_TAB,
        "key.categories.grabby"
    )

    @SubscribeEvent
    fun onRegisterKeyMappings(event: RegisterKeyMappingsEvent) {
        event.register(GRAB_KEY)
        event.register(ROTATE_KEY)
    }
}