package com.blairdash.blueclient.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

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
    private static final Style FONT = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("clientmod", "rank")));
    public static Component text(int rgb) {
        return Component.literal(glyph(rgb)).setStyle(FONT.withColor(TextColor.fromRgb(tint(rgb))).withShadowColor(0));
    }
    public static final Identifier ATLAS = Identifier.fromNamespaceAndPath("clientmod", "textures/font/rank.png");
    /** Invisible 9px spacer (one per rank); the tab list mixin draws the badge texture over it so it gets no text shadow. */
    public static Component spacer(int rgb) {
        return Component.literal(String.valueOf((char) (glyph(rgb).charAt(0) + 0x10))).setStyle(FONT);
    }
    public static int badgeIndex(net.minecraft.util.FormattedCharSequence t) {
        int[] r = { -1 };
        t.accept((i, s, cp) -> { if (cp >= 0xE010 && cp <= 0xE014) r[0] = cp - 0xE010; return false; });
        return r[0];
    }
    public static void drawBadge(GuiGraphicsExtractor c, int idx, int x, int y) {
        GfxCompat.blitRegion(c, ATLAS, x, y, idx * 64f);
    }
    public static void draw(GuiGraphicsExtractor c, Font tr, int x, int y, int rgb) {
        c.text(tr, text(rgb), x, y, 0xFF000000 | tint(rgb), false);
    }
    /** Name-label prefix: badge glyph (shadow-free via Style shadow colour) plus a space. */
    public static Component label(int rgb) {
        return Component.empty().append(text(rgb)).append(Component.literal(" "));
    }}
