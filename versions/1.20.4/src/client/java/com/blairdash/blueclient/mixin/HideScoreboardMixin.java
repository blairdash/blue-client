package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide Scoreboard (1.20.x): skips drawing the sidebar scoreboard. The "numbers only" option needs 1.20.3+ score formatting, so it isn't offered here. */
@Mixin(InGameHud.class)
public class HideScoreboardMixin {

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"), cancellable = true)
    private void clientmod$hideScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (ModConfig.hideScoreboardEnabled) {
            ci.cancel();
        }
    }
}
