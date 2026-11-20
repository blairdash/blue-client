package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide Scoreboard: skips drawing the sidebar scoreboard. */
@Mixin(Gui.class)
public class HideScoreboardMixin {

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void clientmod$hideScoreboard(GuiGraphicsExtractor graphics, DeltaTracker tracker, CallbackInfo ci) {
        if (ModConfig.hideScoreboardEnabled && !com.blairdash.blueclient.mods.LegacyOptions.s("scoreboard_opts", "numbers_only").b) {
            ci.cancel();
        }
    }
    // The formatting call is in the sidebar's synthetic stream mapper, so include synthetic methods.
    @ModifyExpressionValue(method = "*", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/scores/PlayerScoreEntry;formatValue(Lnet/minecraft/network/chat/numbers/NumberFormat;)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent blue$scoreNumbers(MutableComponent original) {
        return ModConfig.hideScoreboardEnabled && com.blairdash.blueclient.mods.LegacyOptions.s("scoreboard_opts", "numbers_only").b
                ? Component.empty() : original;
    }
}