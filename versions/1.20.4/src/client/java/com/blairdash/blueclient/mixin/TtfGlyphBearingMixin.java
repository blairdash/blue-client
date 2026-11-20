package com.blairdash.blueclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Vanilla 1.20.x bug: a TrueType glyph's left offset is (leftSideBearing + bitmapBoxX0 + shift), but the bitmap box
 * x0 from stb already includes the left side bearing, so every glyph is drawn shifted by its own bearing. That gives
 * uneven letter gaps, and a letter with a negative bearing (like "j") lands on top of the one before it.
 * Only TrueType fonts go through this class (Blue's own fonts); the built-in bitmap fonts are untouched.
 */
@Mixin(targets = "net.minecraft.client.font.TrueTypeFont$TtfGlyph")
public class TtfGlyphBearingMixin {
    // constructor floats: advance (ordinal 0), leftSideBearing (ordinal 1)
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static float clientmod$noDoubleBearing(float leftSideBearing) {
        return 0f;
    }
}