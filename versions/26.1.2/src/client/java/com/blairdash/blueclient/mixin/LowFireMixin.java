package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lowers the first-person fire overlay by shifting the matrix down before
 * the overlay is drawn, then restoring it afterwards so the other overlays
 * (underwater, in-wall) aren't moved too. renderFireOverlay is static, so
 * the handlers have to be static as well.
 */
@Mixin(ScreenEffectRenderer.class)
public class LowFireMixin {

    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void clientmod$lowerFireStart(PoseStack matrices, MultiBufferSource vertexConsumers,
                                                 TextureAtlasSprite sprite, CallbackInfo ci) {
        if (ModConfig.lowFireEnabled) {
            matrices.pushPose();
            matrices.translate(0.0F, -ModConfig.lowFireOffset, 0.0F);
        }
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void clientmod$lowerFireEnd(PoseStack matrices, MultiBufferSource vertexConsumers,
                                               TextureAtlasSprite sprite, CallbackInfo ci) {
        if (ModConfig.lowFireEnabled) {
            matrices.popPose();
        }
    }
}