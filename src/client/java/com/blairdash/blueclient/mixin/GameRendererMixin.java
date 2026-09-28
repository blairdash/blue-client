package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.BlueClientInit;
import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void clientmod$applyZoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        if (ModConfig.zoomEnabled && BlueClientInit.zoomKey != null && BlueClientInit.zoomKey.isPressed()) {
            cir.setReturnValue((float) ModConfig.zoomFov);
        }
    }
}