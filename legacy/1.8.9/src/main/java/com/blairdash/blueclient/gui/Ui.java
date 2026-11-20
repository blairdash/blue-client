package com.blairdash.blueclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/**
 * Shared colors and tiny drawing helpers for the Blue Client menu.
 * The palette is the launcher's (index.html :root variables) so both look the same.
 */
final class Ui {
    private Ui() { }

    /** Physical pixels per UI unit while the main menu is drawing (1 elsewhere); rounded shapes use it for 1px curves. */
    static int res = 1;

    static final int DIM = 0x99070A12;
    static final int PANEL = 0xF5111624;
    static final int PANEL_EDGE = 0xFF1F2740;
    static final int HEADER = 0xFF0A0D15;
    static final int SIDEBAR = 0xFF0D1119;
    static final int CARD = 0xFF161C2E;
    static final int CARD_HOVER = 0xFF1B2339;
    static final int SELECTED = 0xFF17233F;
    static final int FIELD = 0xFF0B0E17;
    static final int TRACK_OFF = 0xFF232C47;
    static final int ACCENT = 0xFF3D8BFF;
    static final int ACCENT_DARK = 0xFF2A6FE0;
    static final int GOOD = 0xFF2FD27A;
    static final int GOOD_HOVER = 0xFF4BE08F;
    static final int BAD = 0xFFFF6B6B;
    static final int BAD_HOVER = 0xFFFF8585;
    static final int TEXT = 0xFFE8ECF8;
    static final int MUTED = 0xFF8A93AD;
    static final int DISABLED_TEXT = 0xFF565E78;

    static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    static void fill(int x1, int y1, int x2, int y2, int color) {
        Gui.drawRect(x1, y1, x2, y2, color);
    }

    /** Vertical gradient, drawn as thin strips. */
    static void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        int h = y2 - y1, step = 2;
        for (int i = 0; i < h; i += step) {
            float t = h <= 1 ? 0f : i / (float) (h - 1);
            int c = 0xFF000000;
            for (int sh = 16; sh >= 0; sh -= 8) {
                int a = (top >> sh) & 0xFF, b = (bottom >> sh) & 0xFF;
                c |= (Math.round(a + (b - a) * t) & 0xFF) << sh;
            }
            Gui.drawRect(x1, y1 + i, x2, Math.min(y2, y1 + i + step), c);
        }
    }

    /** Rounded filled rectangle, 3px corner radius. */
    static void rrect(int x, int y, int w, int h, int color) {
        rrect(x, y, w, h, 3, color);
    }

    /** Rounded filled rectangle with an explicit corner radius (1px-precise curves via res). */
    static void rrect(int x, int y, int w, int h, int r, int color) {
        int k = Math.max(1, res);
        if (k == 1) { rrectRaw(x, y, w, h, r, color); return; }
        GlStateManager.pushMatrix();
        GlStateManager.scale(1f / k, 1f / k, 1f);
        rrectRaw(x * k, y * k, w * k, h * k, r * k, color);
        GlStateManager.popMatrix();
    }

    private static void rrectRaw(int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { fill(x, y, x + w, y + h, color); return; }
        // overlap strips by 1px (opaque only) so fractional pixel scaling can't leave seams
        int ov = (color >>> 24) == 0xFF ? 1 : 0;
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * (double) r - dy * dy));
            fill(x + inset, y + i, x + w - inset, y + i + 1 + ov, color);
            fill(x + inset, y + h - 1 - i - ov, x + w - inset, y + h - i, color);
        }
        fill(x, y + r, x + w, y + h - r, color);
    }

    /** Rounded rectangle with a 1px outline, like the launcher's panels. */
    static void card(int x, int y, int w, int h, int r, int fill, int line) {
        rrect(x, y, w, h, r, line);
        rrect(x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    /** Pill toggle switch (on = green track, knob right). */
    static void toggle(int x, int y, boolean on, boolean hover) {
        int w = 26, h = 12;
        rrect(x, y, w, h, 6, on ? (hover ? GOOD_HOVER : GOOD) : (hover ? 0xFF2D3757 : TRACK_OFF));
        int kx = on ? x + w - h + 1 : x + 1;
        rrect(kx, y + 1, h - 2, h - 2, 5, 0xFFFFFFFF);
    }

    static void border(int x, int y, int w, int h, int color) {
        fill(x + 1, y, x + w - 1, y + 1, color);
        fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        fill(x, y + 1, x + 1, y + h - 1, color);
        fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    static int width(String s) { return UiFont.width(s); }

    static void text(FontRenderer fr, String s, int x, int y, int color) {
        UiFont.draw(s, x, y, color);
    }

    static void centered(FontRenderer fr, String s, int cx, int y, int color) {
        UiFont.draw(s, cx - UiFont.width(s) / 2f, y, color);
    }

    /** Centered text inside a box, vertically centered. */
    static void label(FontRenderer fr, String s, int x, int y, int w, int h, int color) {
        UiFont.draw(s, x + (w - UiFont.width(s)) / 2f, y + (h - 8) / 2f, color);
    }

    /** Letter-spaced text (spacing 0 = plain). */
    static void spaced(FontRenderer fr, String s, int x, int y, int color, int spacing) {
        float cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            UiFont.draw(ch, cx, y, color);
            cx += UiFont.width(ch) + spacing;
        }
    }

    static String clip(FontRenderer fr, String s, int maxWidth) {
        if (UiFont.width(s) <= maxWidth) return s;
        String t = s;
        while (t.length() > 1 && UiFont.width(t + "...") > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    /** Clips drawing to a rectangle given in scaled GUI pixels. Pair with {@link #endScissor()}. */
    static void scissor(int x, int y, int w, int h) {
        Minecraft mc = Minecraft.getMinecraft();
        int f = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * f, mc.displayHeight - (y + h) * f, Math.max(0, w * f), Math.max(0, h * f));
    }

    /** Same, but for the main menu whose coordinates are in UI units of {@link #res} physical pixels. */
    static void scissorUi(int x, int y, int w, int h) {
        Minecraft mc = Minecraft.getMinecraft();
        int f = Math.max(1, res);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * f, mc.displayHeight - (y + h) * f, Math.max(0, w * f), Math.max(0, h * f));
    }

    static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /** Brand wordmark: ExtraBold, no extra gaps between letters. */
    static int brandWidth(String s) { return UiFont.brandWidth(s); }
    static void brand(String s, int x, int y, int color) { UiFont.brandDraw(s, x, y, color); }

    private static final java.util.Map<String, net.minecraft.util.ResourceLocation> ICON_CACHE = new java.util.HashMap<String, net.minecraft.util.ResourceLocation>();
    private static final java.util.Map<String, java.awt.image.BufferedImage> ICON_SRC = new java.util.HashMap<String, java.awt.image.BufferedImage>();

    /** Draws a white 256px source icon at exactly units*res screen pixels, area-averaged so it stays smooth at any GUI scale. */
    static void icon(String name, int x, int y, int units) {
        int px = Math.max(8, units * Math.max(1, res));
        String key = name + "_" + px;
        net.minecraft.util.ResourceLocation id = ICON_CACHE.get(key);
        Minecraft mc = Minecraft.getMinecraft();
        if (id == null) {
            try {
                java.awt.image.BufferedImage src = ICON_SRC.get(name);
                if (src == null) {
                    java.io.InputStream in = mc.getResourceManager().getResource(new net.minecraft.util.ResourceLocation("blueclient", "textures/gui/icon_" + name + ".png")).getInputStream();
                    try { src = javax.imageio.ImageIO.read(in); } finally { in.close(); }
                    ICON_SRC.put(name, src);
                }
                java.awt.image.BufferedImage dst = new java.awt.image.BufferedImage(px, px, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                double r = src.getWidth() / (double) px;
                for (int dy = 0; dy < px; dy++) for (int dx = 0; dx < px; dx++) {
                    double x0 = dx * r, x1 = x0 + r, y0 = dy * r, y1 = y0 + r, sum = 0;
                    for (int sy = (int) y0; sy < Math.min(src.getHeight(), (int) Math.ceil(y1)); sy++) {
                        double wy = Math.min(y1, sy + 1) - Math.max(y0, sy);
                        for (int sx = (int) x0; sx < Math.min(src.getWidth(), (int) Math.ceil(x1)); sx++) {
                            double wx = Math.min(x1, sx + 1) - Math.max(x0, sx);
                            sum += wx * wy * ((src.getRGB(sx, sy) >>> 24) & 0xFF);
                        }
                    }
                    int a = (int) Math.round(sum / (r * r));
                    dst.setRGB(dx, dy, (Math.min(255, a) << 24) | 0xFFFFFF);
                }
                net.minecraft.client.renderer.texture.DynamicTexture tex = new net.minecraft.client.renderer.texture.DynamicTexture(dst);
                id = mc.getTextureManager().getDynamicTextureLocation("blueclient_icon_" + key, tex);
            } catch (Throwable ex) { ICON_CACHE.put(key, null); return; }
            ICON_CACHE.put(key, id);
        }
        if (id == null) return;
        mc.getTextureManager().bindTexture(id);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);
        net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, units, units, units, units);
    }
}