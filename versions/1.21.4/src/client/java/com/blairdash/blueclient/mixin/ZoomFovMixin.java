package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.features.ZoomFeature;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides the render-time FOV while zoom is held, in place of writing to
 * the FOV *option* (see ZoomFeature's own comment for why that doesn't
 * work below 30 degrees). getFov has two return points (an early one for
 * the title-screen panorama, then the normal one after death/submersion
 * effects); injecting at both is harmless since ZoomFeature never reports
 * an active override without a real player in a world.
 */
@Mixin(GameRenderer.class)
public class ZoomFovMixin {

    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F", at = @At("RETURN"), cancellable = true)
    private void clientmod$overrideZoomFov(Camera camera, float tickDelta, boolean useFovModifier, CallbackInfoReturnable<Float> cir) {
        double override = ZoomFeature.getZoomFovOverride();
        if (override >= 0) {
            cir.setReturnValue((float) override);
        }
    }
}
