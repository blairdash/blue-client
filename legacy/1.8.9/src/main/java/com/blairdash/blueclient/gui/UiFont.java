package com.blairdash.blueclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * The menu fonts: Outfit Bold for text and Outfit ExtraBold for the brand wordmark (the same TrueType files the
 * Fabric builds use). 1.8.9 can't load TTF fonts, so glyphs are rasterised once at 4x with Java2D into a texture
 * and drawn as scaled, filtered quads. Falls back to Minecraft's own font if a face can't be loaded.
 */
final class UiFont {
    private UiFont() { }

    static final Face UI = new Face("font/outfit-bold.ttf", 8f);
    static final Face BRAND = new Face("font/outfit-extrabold.ttf", 21f);

    static boolean ready() { return UI.ready(); }
    static int width(String s) { return UI.width(s); }
    static void draw(String s, float x, float y, int argb) { UI.draw(s, x, y, argb); }
    static int brandWidth(String s) { return BRAND.width(s); }
    static void brandDraw(String s, float x, float y, int argb) { BRAND.draw(s, x, y, argb); }

    static final class Face {
        private static final int SCALE = 4;
        private static final int FIRST = 32, LAST = 255, PAD = 3;
        private final String path;
        private final float size;
        private boolean tried, ok;
        private ResourceLocation atlas;
        private int atlasW, atlasH, cellH, ascent;
        private final float[] adv = new float[LAST + 1];
        private final int[] gx = new int[LAST + 1], gy = new int[LAST + 1];

        Face(String path, float size) { this.path = path; this.size = size; }

        boolean ready() {
            if (!tried) { tried = true; try { build(); ok = true; } catch (Throwable t) { ok = false; System.err.println("[blueclient] UI font unavailable: " + t); } }
            return ok;
        }

        private void build() throws Exception {
            Minecraft mc = Minecraft.getMinecraft();
            InputStream in = mc.getResourceManager().getResource(new ResourceLocation("blueclient", path)).getInputStream();
            Font base = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(size * SCALE);
            in.close();
            BufferedImage probe = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
            Graphics2D pg = probe.createGraphics();
            applyHints(pg);
            pg.setFont(base);
            FontMetrics fm = pg.getFontMetrics();
            ascent = fm.getAscent();
            cellH = fm.getAscent() + fm.getDescent() + PAD * 2;
            int x = PAD, y = 0, rowW = 2048;
            for (int c = FIRST; c <= LAST; c++) {
                int w = fm.charWidth((char) c);
                adv[c] = w;
                int cw = w + PAD * 2;
                if (x + cw > rowW) { x = PAD; y += cellH; }
                gx[c] = x - PAD; gy[c] = y;
                x += cw;
            }
            pg.dispose();
            atlasW = rowW;
            atlasH = y + cellH;
            BufferedImage img = new BufferedImage(atlasW, atlasH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            applyHints(g);
            g.setFont(base);
            g.setColor(java.awt.Color.WHITE);
            for (int c = FIRST; c <= LAST; c++) g.drawString(String.valueOf((char) c), gx[c] + PAD, gy[c] + PAD + ascent);
            g.dispose();
            DynamicTexture tex = new DynamicTexture(img);
            atlas = mc.getTextureManager().getDynamicTextureLocation("blueclient_uifont_" + (int) size, tex);
            GlStateManager.bindTexture(tex.getGlTextureId());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        }

        private static void applyHints(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        private static int idx(char c) { return c < FIRST || c > LAST ? '?' : c; }

        int width(String s) {
            if (!ready()) return Minecraft.getMinecraft().fontRendererObj.getStringWidth(s);
            float w = 0;
            for (int i = 0; i < s.length(); i++) w += adv[idx(s.charAt(i))];
            return Math.round(w / SCALE);
        }

        /** Draws the text with its top edge near y (matches the line box the vanilla font would use). */
        void draw(String s, float x, float y, int argb) {
            if (!ready()) { Minecraft.getMinecraft().fontRendererObj.drawString(s, (int) x, (int) y, argb); return; }
            Minecraft.getMinecraft().getTextureManager().bindTexture(atlas);
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            float a = ((argb >> 24) & 0xFF) / 255f;
            if (a == 0f) a = 1f;
            GlStateManager.color(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, a);
            Tessellator t = Tessellator.getInstance();
            WorldRenderer w = t.getWorldRenderer();
            w.begin(7, DefaultVertexFormats.POSITION_TEX);
            float cx = x;
            float top = y - 1.5f * size / 8f;
            float gh = cellH / (float) SCALE;
            for (int i = 0; i < s.length(); i++) {
                int c = idx(s.charAt(i));
                float cw = (adv[c] + PAD * 2) / SCALE, ox = cx - PAD / (float) SCALE;
                float u0 = gx[c] / (float) atlasW, u1 = (gx[c] + adv[c] + PAD * 2) / atlasW;
                float v0 = gy[c] / (float) atlasH, v1 = (gy[c] + cellH) / (float) atlasH;
                w.pos(ox, top, 0).tex(u0, v0).endVertex();
                w.pos(ox, top + gh, 0).tex(u0, v1).endVertex();
                w.pos(ox + cw, top + gh, 0).tex(u1, v1).endVertex();
                w.pos(ox + cw, top, 0).tex(u1, v0).endVertex();
                cx += adv[c] / SCALE;
            }
            t.draw();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }
}