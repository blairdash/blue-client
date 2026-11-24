package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.gui.RankIcon;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the Blue Client badge as a texture (so it never gets the text drop shadow) over the invisible spacer that starts a name. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class TextBadgeMixin {
    @Inject(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"))
    private void blueclient$badge(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        int idx = RankIcon.badgeIndex(text);
        if (idx >= 0) RankIcon.drawBadge((GuiGraphicsExtractor) (Object) this, idx, x, y);
    }
}