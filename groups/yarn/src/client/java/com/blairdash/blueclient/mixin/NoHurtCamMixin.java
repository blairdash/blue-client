package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Hurt Cam: skips the camera tilt/shake applied when you take damage. */
@Mixin(GameRenderer.class)
public class NoHurtCamMixin {

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void clientmod$noHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (ModConfig.noHurtCamEnabled && com.blairdash.blueclient.mods.LegacyOptions.s("hurtcam_opts", "intensity").n <= 0) {
            ci.cancel();
        }
    }
    @ModifyArg(method = "tiltViewWhenHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/math/RotationAxis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 0), index = 0)
    private float blue$deathIntensity(float angle) { return blue$intensity(angle); }

    @ModifyArg(method = "tiltViewWhenHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/math/RotationAxis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 2), index = 0)
    private float blue$hurtIntensity(float angle) { return blue$intensity(angle); }

    private float blue$intensity(float angle) {
        return ModConfig.noHurtCamEnabled
                ? angle * (float) com.blairdash.blueclient.mods.LegacyOptions.s("hurtcam_opts", "intensity").n : angle;
    }
}
