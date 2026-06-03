package net.joth.grabby.mixin;

import net.joth.grabby.client.GrabbyClientEvents;
import net.joth.grabby.client.GrabbyHoldingPlayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void grabby$applyHoldingPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;

        boolean isHolding = player.getUUID().equals(mc.player.getUUID())
            ? GrabbyClientEvents.isHolding
            : GrabbyHoldingPlayers.players.contains(player.getUUID());

        if (!isHolding) return;

        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        model.leftArm.zRot = 0.0f;
        model.rightArm.zRot = 0.0f;
        model.leftArm.xRot = (float) Math.toRadians(-80.0) + model.head.xRot;
        model.rightArm.xRot = (float) Math.toRadians(-80.0) + model.head.xRot;
        model.rightArm.yRot = (float) Math.toRadians(-15.0);
        model.leftArm.yRot = (float) Math.toRadians(15.0);
    }
}
