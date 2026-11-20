package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MobSizeMixin {
    @Inject(method = "scale", at = @At("HEAD"), require = 0)
    private void blue$mobSize(LivingEntityRenderState state, MatrixStack matrices, CallbackInfo ci) {
        if (state instanceof PlayerEntityRenderState) return;
        BlueMod m = VisualMods.active("mob_size");
        if (m != null) { float s = (float) m.setting("scale").n; matrices.scale(s, s, s); }
    }
}
