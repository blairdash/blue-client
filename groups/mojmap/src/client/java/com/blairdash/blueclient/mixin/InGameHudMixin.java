package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class InGameHudMixin {

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void clientmod$cancelVanillaCrosshair(GuiGraphicsExtractor drawContext, DeltaTracker tickCounter, CallbackInfo ci) {
        if (ModConfig.customCrosshairEnabled) {
            ci.cancel();
        }
    }

    // Hides vanilla's effect icons (top-right) while our Potion HUD is on,
    // so the two don't show the same information twice.
    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void clientmod$cancelVanillaEffects(GuiGraphicsExtractor drawContext, DeltaTracker tickCounter, CallbackInfo ci) {
        if (ModConfig.potionHudEnabled) {
            ci.cancel();
        }
    }
}