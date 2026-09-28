package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
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
@Mixin(InGameOverlayRenderer.class)
public class LowFireMixin {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void clientmod$lowerFireStart(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                                 Sprite sprite, CallbackInfo ci) {
        if (ModConfig.lowFireEnabled) {
            matrices.push();
            matrices.translate(0.0F, -ModConfig.lowFireOffset, 0.0F);
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private static void clientmod$lowerFireEnd(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                               Sprite sprite, CallbackInfo ci) {
        if (ModConfig.lowFireEnabled) {
            matrices.pop();
        }
    }
}