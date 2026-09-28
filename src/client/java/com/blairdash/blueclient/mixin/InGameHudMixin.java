package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void clientmod$cancelVanillaCrosshair(DrawContext drawContext, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (ModConfig.customCrosshairEnabled) {
            ci.cancel();
        }
    }

    // Hides vanilla's effect icons (top-right) while our Potion HUD is on,
    // so the two don't show the same information twice.
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void clientmod$cancelVanillaEffects(DrawContext drawContext, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (ModConfig.potionHudEnabled) {
            ci.cancel();
        }
    }
}