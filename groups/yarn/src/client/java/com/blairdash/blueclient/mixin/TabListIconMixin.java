package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.gui.RankIcon;
import com.blairdash.blueclient.net.RankService;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rank icon in front of Blue Client players' names in the tab list, on every server. */
@Mixin(PlayerListHud.class)
public abstract class TabListIconMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void blueclient$rankIcon(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Integer color = RankService.colorFor(entry.getProfile().id());
        if (color == null) return;
        cir.setReturnValue(Text.empty().append(RankIcon.spacer(color)).append(cir.getReturnValue()));
    }
}