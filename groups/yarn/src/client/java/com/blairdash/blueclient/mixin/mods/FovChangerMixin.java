package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class FovChangerMixin {
    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)F", at = @At("RETURN"), cancellable = true)
    private void blue$fov(Camera camera, float tickDelta, boolean useFovModifier, CallbackInfoReturnable<Float> cir) {
        BlueMod m = VisualMods.active("fov_changer");
        if (m != null) cir.setReturnValue((float) Math.min(170.0, cir.getReturnValueF() * m.setting("mult").n));
    }
}
