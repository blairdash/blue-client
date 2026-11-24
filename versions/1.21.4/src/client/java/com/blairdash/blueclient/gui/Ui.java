package com.blairdash.blueclient.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Shared colors and tiny drawing helpers for the Blue Client menu.
 * The palette is the launcher's (index.html :root variables) so both look the same.
 */
final class Ui {
    private Ui() { }

    /** Physical pixels per UI unit while the Click GUI is drawing (1 elsewhere); rounded shapes use it for 1px curves. */
    static int res = 1;

    /** Physical pixels per UI unit inside the scaled menus (set next to {@link #res}); 0 = plain GUI units. */
    static int unitPx = 0;
    /** GUI scale factor used by the Click GUI (set by the screen). */
    static float k = 1f;

    private static final Style[] FONTS = new Style[9];
    private static final Style[] BRAND_FONTS = new Style[9];

    /** Minecraft puts the text baseline a fixed 7 font pixels below y, so a larger font needs the difference added back. */
    private static final int BASELINE = 7;

    /** Height of the brand wordmark font in layout units, about 2.6 times the 8-unit menu text. */
    private static final int BRAND_UNITS = 21;

    /** Screen pixels per layout unit, 1..8. Fonts ui1..ui8 are the same face at 8 * n pixels, so one font pixel is one screen pixel. */
    private static int px() {
        int g = unitPx > 0 ? unitPx : (int) Math.round(MinecraftClient.getInstance().getWindow().getScaleFactor());
        return Math.max(1, Math.min(8, g));
    }

    private static Text t(String s, int g) { return t(s, g, false); }
    static Text t(String s) { return t(s, px()); }

    /** The brand wordmark uses the ExtraBold face (fonts brand1..brand8), everything else the Bold face (ui1..ui8). */
    private static Text t(String s, int g, boolean brand) {
        Style[] cache = brand ? BRAND_FONTS : FONTS;
        Style st = cache[g];
        if (st == null) cache[g] = st = Style.EMPTY.withFont(Identifier.of("clientmod", (brand ? "brand" : "ui") + g));
        return Text.literal(s).setStyle(st);
    }

    static int width(TextRenderer tr, String s) {
        int g = px();
        return Math.round(tr.getWidth(t(s, g)) / (float) g);
    }

    /**
     * Text is rasterised at the real screen size and drawn on whole screen pixels. Minecraft snaps glyph advances
     * to whole font pixels, so with one font pixel per screen pixel the letter spacing is exact at any GUI scale.
     */
    static void text(DrawContext c, TextRenderer tr, String s, int x, int y, int color) {
        int g = px();
        c.getMatrices().push();
        c.getMatrices().scale(1f / g, 1f / g, 1f);
        c.drawText(tr, t(s, g), x * g, y * g + BASELINE * (g - 1), color, false);
        c.getMatrices().pop();
    }
    // launcher: --bg #0b0e17, --panel #111624, --panel2 #161c2e, --line #1f2740
    static final int DIM = 0x99070A12;
    static final int PANEL = 0xF5111624;
    static final int PANEL_EDGE = 0xFF1F2740;   // --line
    static final int HEADER = 0xFF0A0D15;       // title bar / rail
    static final int SIDEBAR = 0xFF0D1119;
    static final int CARD = 0xFF161C2E;         // --panel2
    static final int CARD_HOVER = 0xFF1B2339;
    static final int SELECTED = 0xFF17233F;     // active rail/profile row
    static final int FIELD = 0xFF0B0E17;        // --bg
    static final int TRACK_OFF = 0xFF232C47;
    static final int ACCENT = 0xFF3D8BFF;       // --accent
    static final int ACCENT_DARK = 0xFF2A6FE0;  // --accent2
    static final int GOOD = 0xFF2FD27A;         // --good
    static final int GOOD_HOVER = 0xFF4BE08F;
    static final int BAD = 0xFFFF6B6B;          // --bad
    static final int BAD_HOVER = 0xFFFF8585;
    static final int TEXT = 0xFFE8ECF8;         // --text
    static final int MUTED = 0xFF8A93AD;        // --muted
    static final int DISABLED_TEXT = 0xFF565E78;

    static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Rounded filled rectangle, 3px corner radius. */
    static void rrect(DrawContext c, int x, int y, int w, int h, int color) {
        rrect(c, x, y, w, h, 3, color);
    }

    /** Rounded filled rectangle with an explicit corner radius. */
    static void rrect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        int k = Math.max(1, res);
        if (k == 1) { rrectRaw(c, x, y, w, h, r, color); return; }
        c.getMatrices().push();
        c.getMatrices().scale(1f / k, 1f / k, 1f);
        rrectRaw(c, x * k, y * k, w * k, h * k, r * k, color);
        c.getMatrices().pop();
    }

    private static void rrectRaw(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { c.fill(x, y, x + w, y + h, color); return; }
        // overlap strips by 1px (opaque only) so fractional pixel scaling can't leave seams
        int ov = (color >>> 24) == 0xFF ? 1 : 0;
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * (double) r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1 + ov, color);
            c.fill(x + inset, y + h - 1 - i - ov, x + w - inset, y + h - i, color);
        }
        c.fill(x, y + r, x + w, y + h - r, color);
    }

    /** Rounded rectangle with a 1px outline, like the launcher's panels. */
    static void card(DrawContext c, int x, int y, int w, int h, int r, int fill, int line) {
        rrect(c, x, y, w, h, r, line);
        rrect(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    /** Pill toggle switch (on = green track, knob right). */
    static void toggle(DrawContext c, int x, int y, boolean on, boolean hover) {
        int w = 26, h = 12;
        rrect(c, x, y, w, h, 6, on ? (hover ? GOOD_HOVER : GOOD) : (hover ? 0xFF2D3757 : TRACK_OFF));
        int kx = on ? x + w - h + 1 : x + 1;
        rrect(c, kx, y + 1, h - 2, h - 2, 5, 0xFFFFFFFF);
    }

    static void border(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x + 1, y, x + w - 1, y + 1, color);
        c.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        c.fill(x, y + 1, x + 1, y + h - 1, color);
        c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    static void centered(DrawContext c, TextRenderer tr, String s, int cx, int y, int color) {
        text(c, tr, s, cx - width(tr, s) / 2, y, color);
    }

    /** Centered text inside a box, vertically centered. */
    static void label(DrawContext c, TextRenderer tr, String s, int x, int y, int w, int h, int color) {
        text(c, tr, s, x + (w - width(tr, s)) / 2, y + (h - 8) / 2, color);
    }

    /** Letter-spaced text (the launcher's uppercase brand/heading look). */
    static void spaced(DrawContext c, TextRenderer tr, String s, int x, int y, int color, int spacing) {
        int g = px();
        int penX = x * g;
        c.getMatrices().push();
        c.getMatrices().scale(1f / g, 1f / g, 1f);
        for (int i = 0; i < s.length(); i++) {
            Text ch = t(String.valueOf(s.charAt(i)), g);
            c.drawText(tr, ch, penX, y * g + BASELINE * (g - 1), color, false);
            penX += tr.getWidth(ch) + spacing * g;
        }
        c.getMatrices().pop();
    }

    static int spacedWidth(TextRenderer tr, String s, int spacing) {
        int g = px();
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += tr.getWidth(t(String.valueOf(s.charAt(i)), g)) + spacing * g;
        return Math.round(Math.max(0, w - spacing * g) / (float) g);
    }
    /** Brand wordmark: ExtraBold, with no extra gaps between the letters. */
    static void brand(DrawContext c, TextRenderer tr, String s, int x, int y, int color) {
        int g = px();
        c.getMatrices().push();
        c.getMatrices().scale(1f / g, 1f / g, 1f);
        c.drawText(tr, t(s, g, true), x * g, y * g + Math.round(BRAND_UNITS * 7f / 8f * g) - BASELINE, color, false);
        c.getMatrices().pop();
    }

    /** Smaller brand wordmark (k times the normal size), used in the Click GUI title bar. */
    static void brandSmall(DrawContext c, TextRenderer tr, String s, int x, int y, int color, float k) {
        int g = px();
        c.getMatrices().push();
        c.getMatrices().scale(k / g, k / g, 1f);
        c.drawText(tr, t(s, g, true), Math.round(x * g / k), Math.round(y * g / k) + Math.round(BRAND_UNITS * 7f / 8f * g) - BASELINE, color, false);
        c.getMatrices().pop();
    }

    /** The Blue Client logo: rounded square with the diagonal blue gradient, drawn at whole screen pixels. */
    static void logo(DrawContext c, int x, int y, int units) {
        int g = px();
        int S = units * g;
        float R = S * 0.24f;
        int chunk = Math.max(1, S / 14);
        c.getMatrices().push();
        c.getMatrices().scale(1f / g, 1f / g, 1f);
        for (int row = 0; row < S; row++) {
            float dy = row < R ? R - row - 0.5f : (row >= S - R ? row - (S - R) + 0.5f : 0f);
            int inset = dy > 0f ? Math.round(R - (float) Math.sqrt(Math.max(0f, R * R - dy * dy))) : 0;
            for (int col = inset; col < S - inset; col += chunk) {
                int end = Math.min(S - inset, col + chunk);
                float tt = Math.min(1f, Math.max(0f, ((col + end) / 2f + row) / (2f * S)));
                int rr = Math.round(0x12 + (0x46 - 0x12) * tt), gg = Math.round(0xBA + (0x90 - 0xBA) * tt), bb = Math.round(0xFF + (0xF0 - 0xFF) * tt);
                c.fill(x * g + col, y * g + row, x * g + end + (end < S - inset ? 1 : 0), y * g + row + (row < S - 1 ? 2 : 1), 0xFF000000 | (rr << 16) | (gg << 8) | bb);
            }
        }
        c.getMatrices().pop();
    }

    private static final java.util.Map<String, net.minecraft.util.Identifier> ICON_CACHE = new java.util.HashMap<>();
    private static final java.util.Map<String, net.minecraft.client.texture.NativeImage> ICON_SRC = new java.util.HashMap<>();

    /** Draws a white 256px source icon at exactly units*unitPx screen pixels, area-averaged so it stays smooth at any GUI scale. */
    static void icon(DrawContext c, String name, int x, int y, int units) {
        int px = Math.max(8, units * px2());
        String key = name + "_" + px;
        net.minecraft.util.Identifier id = ICON_CACHE.get(key);
        if (id == null) {
            id = net.minecraft.util.Identifier.of("clientmod", "dyn/icon_" + key);
            try {
                net.minecraft.client.texture.NativeImage src = ICON_SRC.get(name);
                if (src == null) {
                    var res = MinecraftClient.getInstance().getResourceManager().getResource(net.minecraft.util.Identifier.of("clientmod", "textures/gui/icon_" + name + ".png"));
                    try (java.io.InputStream in = res.get().getInputStream()) { src = net.minecraft.client.texture.NativeImage.read(in); }
                    ICON_SRC.put(name, src);
                }
                net.minecraft.client.texture.NativeImage dst = new net.minecraft.client.texture.NativeImage(px, px, false);
                double r = src.getWidth() / (double) px;
                for (int dy = 0; dy < px; dy++) for (int dx = 0; dx < px; dx++) {
                    double x0 = dx * r, x1 = x0 + r, y0 = dy * r, y1 = y0 + r, sum = 0;
                    for (int sy = (int) y0; sy < Math.min(src.getHeight(), (int) Math.ceil(y1)); sy++) {
                        double wy = Math.min(y1, sy + 1) - Math.max(y0, sy);
                        for (int sx = (int) x0; sx < Math.min(src.getWidth(), (int) Math.ceil(x1)); sx++) {
                            double wx = Math.min(x1, sx + 1) - Math.max(x0, sx);
                            sum += wx * wy * ((src.getColorArgb(sx, sy) >>> 24) & 0xFF);
                        }
                    }
                    int a = (int) Math.round(sum / (r * r));
                    dst.setColorArgb(dx, dy, (Math.min(255, a) << 24) | 0xFFFFFF);
                }
                final net.minecraft.client.texture.NativeImage fin = dst;
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, new net.minecraft.client.texture.NativeImageBackedTexture(fin));
            } catch (Throwable ex) { ICON_CACHE.put(key, id); return; }
            ICON_CACHE.put(key, id);
        }
        c.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, id, x, y, 0f, 0f, units, units, px, px, px, px);
    }
    private static int px2() { return unitPx > 0 ? unitPx : (int) Math.round(MinecraftClient.getInstance().getWindow().getScaleFactor()); }
    static int brandWidth(TextRenderer tr, String s) {
        int g = px();
        return Math.round(tr.getWidth(t(s, g, true)) / (float) g);
    }
    static String clip(TextRenderer tr, String s, int maxWidth) {
        if (width(tr, s) <= maxWidth) return s;
        String t = s;
        while (t.length() > 1 && width(tr, t + "...") > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    static void scissor(DrawContext c, int x1, int y1, int x2, int y2) {
        c.enableScissor(x1, y1, x2, y2); // 1.21.4 transforms scissor by the current matrix
    }
}
