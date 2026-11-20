package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.20.x: GameRenderer#getFov returns a double (it became a float in 1.21). */
@Mixin(GameRenderer.class)
public class FovChangerMixin {
    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D", at = @At("RETURN"), cancellable = true)
    private void blue$fov(Camera camera, float tickDelta, boolean useFovModifier, CallbackInfoReturnable<Double> cir) {
        BlueMod m = VisualMods.active("fov_changer");
        if (m != null) cir.setReturnValue(Math.min(170.0, cir.getReturnValueD() * m.setting("mult").n));
    }
}
