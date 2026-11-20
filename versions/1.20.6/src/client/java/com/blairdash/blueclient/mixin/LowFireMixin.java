package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lowers the first-person fire overlay (1.20 signature: renderFireOverlay(MinecraftClient, MatrixStack)). */
@Mixin(InGameOverlayRenderer.class)
public class LowFireMixin {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void clientmod$lowerFireStart(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (ModConfig.lowFireEnabled) {
            matrices.push();
            matrices.translate(0.0F, -ModConfig.lowFireOffset, 0.0F);
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private static void clientmod$lowerFireEnd(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (ModConfig.lowFireEnabled) {
            matrices.pop();
        }
    }
}
