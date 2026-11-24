package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.gui.RankIcon;
import com.blairdash.blueclient.net.RankService;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rank icon in front of Blue Client players' names in the tab list, on every server. */
@Mixin(PlayerTabOverlay.class)
public abstract class TabListIconMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void blueclient$rankIcon(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        Integer color = RankService.colorFor(info.getProfile().id());
        if (color == null) return;
        Component original = cir.getReturnValue();
        // icon is a sibling, not the parent, so the name keeps the default font
        cir.setReturnValue(Component.empty().append(RankIcon.spacer(color)).append(original));
    }
}