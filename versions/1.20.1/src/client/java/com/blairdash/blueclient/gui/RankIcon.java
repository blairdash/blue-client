package com.blairdash.blueclient.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

/** The Blue Client icon as a tintable one-glyph font; colored by the player's rank. */
public final class RankIcon {
    private RankIcon() { }
    /** Per-rank baked gradient glyphs: user = the app icon, content_creator = gold, developer = cyan; anything else = tinted gray gradient. */
    private static String glyph(int rgb) {
        switch (rgb & 0xFFFFFF) { case 0x3B82F6: return "\uE000"; case 0xFFD700: return "\uE001"; case 0x7DF9FF: return "\uE002"; case 0xFF69B4: return "\uE004"; default: return "\uE003"; }
    }
    private static int tint(int rgb) {
        int c = rgb & 0xFFFFFF;
        return (c == 0x3B82F6 || c == 0xFFD700 || c == 0x7DF9FF || c == 0xFF69B4) ? 0xFFFFFF : c;
    }
    public static final int WIDTH = 8;
    private static final Style FONT = Style.EMPTY.withFont(Identifier.of("clientmod", "rank"));
    public static Text text(int rgb) {
        return Text.literal(glyph(rgb)).setStyle(FONT.withColor(TextColor.fromRgb(tint(rgb))));
    }
    public static void draw(DrawContext c, TextRenderer tr, int x, int y, int rgb) {
        c.drawText(tr, text(rgb), x, y, 0xFF000000 | tint(rgb), false);
    }
}
