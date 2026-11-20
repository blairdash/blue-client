package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.features.ZoomFeature;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides the camera's per-frame FOV while zooming, bypassing the
 * options.fov() [30, 110] clamp entirely -- see ZoomFeature for why
 * that clamp made the options-based approach impossible for zoomFov
 * values below 30.
 */
@Mixin(Camera.class)
public class CameraMixin {

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void clientmod$applyZoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        double override = ZoomFeature.currentOverride();
        if (override >= 0) {
            cir.setReturnValue((float) override);
        }
    }
}
