package net.joth.grabby.mixin;

import net.joth.grabby.client.GrabbyRotateState;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    /*
     * Here's how this works. When we are in rotation mode, we intercept the camera rotation
     * with this mixin and store the mouse movement instead of pssing it to the player.
     * This locks the camera and lets us apply rotation to the sublevel
     *
     * Anyway, mixins are so fucking hard man this took me like over an hour
     */
    @Redirect(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void grabby$redirectTurn(LocalPlayer player, double yaw, double pitch) {
        if (GrabbyRotateState.active) {
            GrabbyRotateState.pendingYaw += yaw;
            GrabbyRotateState.pendingPitch += pitch;
        } else {
            player.turn(yaw, pitch);
        }
    }
}
