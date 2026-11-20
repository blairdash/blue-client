package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Hurt Cam: skips the camera tilt/shake applied when you take damage. */
@Mixin(GameRenderer.class)
public class NoHurtCamMixin {

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void clientmod$noHurtCam(CameraRenderState camera, PoseStack poseStack, CallbackInfo ci) {
        if (ModConfig.noHurtCamEnabled && com.blairdash.blueclient.mods.LegacyOptions.s("hurtcam_opts", "intensity").n <= 0) {
            ci.cancel();
        }
    }
    @ModifyArg(method = "bobHurt", at = @At(value = "INVOKE",
            target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 0), index = 0)
    private float blue$deathIntensity(float angle) { return blue$intensity(angle); }

    @ModifyArg(method = "bobHurt", at = @At(value = "INVOKE",
            target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 2), index = 0)
    private float blue$hurtIntensity(float angle) { return blue$intensity(angle); }

    private float blue$intensity(float angle) {
        return ModConfig.noHurtCamEnabled
                ? angle * (float) com.blairdash.blueclient.mods.LegacyOptions.s("hurtcam_opts", "intensity").n : angle;
    }
}