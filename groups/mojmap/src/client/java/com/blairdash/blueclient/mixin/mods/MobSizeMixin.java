package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MobSizeMixin {
    @Inject(method = "scale", at = @At("HEAD"), require = 0)
    private void blue$mobSize(LivingEntityRenderState state, PoseStack poseStack, CallbackInfo ci) {
        if (state instanceof AvatarRenderState) return;
        BlueMod m = VisualMods.active("mob_size");
        if (m != null) { float s = (float) m.setting("scale").n; poseStack.scale(s, s, s); }
    }
}
