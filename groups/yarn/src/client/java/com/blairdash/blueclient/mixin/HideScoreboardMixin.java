package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide Scoreboard: skips drawing the sidebar scoreboard. */
@Mixin(InGameHud.class)
public class HideScoreboardMixin {

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"), cancellable = true)
    private void clientmod$hideScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (ModConfig.hideScoreboardEnabled && !com.blairdash.blueclient.mods.LegacyOptions.s("scoreboard_opts", "numbers_only").b) {
            ci.cancel();
        }
    }
    // The formatting call is in the sidebar's synthetic stream mapper, so include synthetic methods.
    @ModifyExpressionValue(method = "*", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/scoreboard/ScoreboardEntry;formatted(Lnet/minecraft/scoreboard/number/NumberFormat;)Lnet/minecraft/text/MutableText;"))
    private MutableText blue$scoreNumbers(MutableText original) {
        return ModConfig.hideScoreboardEnabled && com.blairdash.blueclient.mods.LegacyOptions.s("scoreboard_opts", "numbers_only").b
                ? Text.empty() : original;
    }
}