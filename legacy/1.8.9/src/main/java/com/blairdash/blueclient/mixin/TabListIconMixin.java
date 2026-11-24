package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.handler.RankNametags;
import com.blairdash.blueclient.net.RankService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Blue Client rank icon in front of Blue Client players' names in the tab list, on every server.
 * The name gets leading spaces to make room, and the icon is drawn on top of that gap.
 */
@Mixin(GuiPlayerTabOverlay.class)
public abstract class TabListIconMixin {
    private static final String PAD = "   ";
    private static final Map<String, Integer> blueclient$colors = new HashMap<String, Integer>();

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void blueclient$pad(NetworkPlayerInfo info, CallbackInfoReturnable<String> cir) {
        if (info == null || info.getGameProfile() == null || info.getGameProfile().getId() == null) return;
        Integer color = RankService.colorFor(info.getGameProfile().getId());
        if (color == null) return;
        String padded = PAD + cir.getReturnValue();
        if (blueclient$colors.size() > 512) blueclient$colors.clear();
        blueclient$colors.put(padded, color);
        cir.setReturnValue(padded);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private int blueclient$drawName(FontRenderer fr, String text, float x, float y, int color) {
        Integer rank = text == null ? null : blueclient$colors.get(text);
        if (rank != null) RankNametags.drawIcon(Minecraft.getMinecraft(), rank, 1f, (int) x, (int) y);
        return fr.drawStringWithShadow(text, x, y, color);
    }
}