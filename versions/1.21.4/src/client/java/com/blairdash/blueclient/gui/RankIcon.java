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
        return Text.literal(glyph(rgb)).setStyle(FONT.withColor(TextColor.fromRgb(tint(rgb))).withShadowColor(0));
    }
    public static final Identifier ATLAS = Identifier.of("clientmod", "textures/font/rank.png");
    /** Invisible 9px spacer (one per rank); the tab list mixin draws the badge texture over it so it gets no text shadow. */
    public static Text spacer(int rgb) {
        return Text.literal(String.valueOf((char) (glyph(rgb).charAt(0) + 0x10))).setStyle(FONT);
    }
    public static int badgeIndex(net.minecraft.text.OrderedText t) {
        int[] r = { -1 };
        t.accept((i, s, cp) -> { if (cp >= 0xE010 && cp <= 0xE014) r[0] = cp - 0xE010; return false; });
        return r[0];
    }
    public static final Identifier FONT_ID = Identifier.of("clientmod", "rank");
    /** Lunar-style badge: called by the text drawer when it reaches a spacer glyph; emits the badge as a textured quad (no text shadow). */
    public static void worldBadge(net.minecraft.client.render.VertexConsumerProvider vcp, net.minecraft.client.font.TextRenderer.TextLayerType type,
                                  org.joml.Matrix4f m, float x, float y, float alpha, int light, Style style, int cp) {
        if (cp < 0xE010 || cp > 0xE014 || !FONT_ID.equals(style.getFont())) return;
        int idx = cp - 0xE010;
        net.minecraft.client.render.RenderLayer layer = switch (type) {
            case SEE_THROUGH -> net.minecraft.client.render.RenderLayer.getTextSeeThrough(ATLAS);
            case POLYGON_OFFSET -> net.minecraft.client.render.RenderLayer.getTextPolygonOffset(ATLAS);
            default -> net.minecraft.client.render.RenderLayer.getText(ATLAS);
        };
        net.minecraft.client.render.VertexConsumer vc = vcp.getBuffer(layer);
        float v0 = idx * 64f / 320f, v1 = (idx + 1) * 64f / 320f, x1 = x + 8f, y1 = y + 8f;
        vc.vertex(m, x, y, 0f).color(1f, 1f, 1f, alpha).texture(0f, v0).light(light);
        vc.vertex(m, x, y1, 0f).color(1f, 1f, 1f, alpha).texture(0f, v1).light(light);
        vc.vertex(m, x1, y1, 0f).color(1f, 1f, 1f, alpha).texture(1f, v1).light(light);
        vc.vertex(m, x1, y, 0f).color(1f, 1f, 1f, alpha).texture(1f, v0).light(light);
    }
    public static void drawBadge(DrawContext c, int idx, int x, int y) {
        c.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, ATLAS, x, y, 0f, idx * 64f, 8, 8, 64, 64, 64, 320);
    }
    public static void draw(DrawContext c, TextRenderer tr, int x, int y, int rgb) {
        c.drawText(tr, text(rgb), x, y, 0xFF000000 | tint(rgb), false);
    }
    /** Name-label prefix: badge glyph (shadow-free via Style shadow colour) plus a space. */
    public static Text label(int rgb) {
        return Text.empty().append(text(rgb)).append(Text.literal(" "));
    }}
