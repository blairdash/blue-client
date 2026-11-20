package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide Scoreboard: skips drawing the sidebar scoreboard. */
@Mixin(GuiIngame.class)
public abstract class HideScoreboardMixin {

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void blueclient$hideScoreboard(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci) {
        if (ModConfig.hideScoreboardEnabled) {
            ci.cancel();
        }
    }
}