package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.gui.RankIcon;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the Blue Client badge as a texture (so it never gets the text drop shadow) over the invisible spacer that starts a name. */
@Mixin(DrawContext.class)
public abstract class TextBadgeMixin {
    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V", at = @At("HEAD"))
    private void blueclient$badge(TextRenderer tr, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        int idx = RankIcon.badgeIndex(text);
        if (idx >= 0) RankIcon.drawBadge((DrawContext) (Object) this, idx, x, y);
    }
}