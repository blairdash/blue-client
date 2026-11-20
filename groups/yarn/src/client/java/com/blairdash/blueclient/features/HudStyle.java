package com.blairdash.blueclient.features;

import com.blairdash.blueclient.mods.Draw;
import com.blairdash.blueclient.mods.LegacyOptions;
import com.blairdash.blueclient.mods.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Draws the older text HUDs (CPS/FPS, Coordinates, Potion HUD, Info HUDs) using the shared "HUD Style" options. */
public final class HudStyle {
    private HudStyle() { }

    private static Setting s(String id) { return LegacyOptions.s("hudstyle_opts", id); }
    public static float scale() { return (float) s("scale").n; }

    /** On-screen width of one line, scale and brackets included. */
    public static int width(MinecraftClient c, String text) {
        int w = Draw.width(c, text);
        if (s("brackets").b) w += Draw.width(c, "[") + Draw.width(c, "]");
        return Math.round(w * scale());
    }

    public static int height(int lines) { return Math.round(lines * 10 * scale()); }

    public static void line(DrawContext ctx, MinecraftClient c, String text, int x, int y) {
        block(ctx, c, List.of(text), null, x, y);
    }

    /** colors[i], when non-zero, replaces the text color of line i. */
    public static void block(DrawContext ctx, MinecraftClient c, List<String> lines, int[] colors, int x, int y) {
        if (lines.isEmpty()) return;
        float sc = scale();
        boolean br = s("brackets").b, shadow = s("shadow").b;
        int lb = br ? Draw.width(c, "[") : 0, rb = br ? Draw.width(c, "]") : 0;
        int w = 0;
        for (String l : lines) w = Math.max(w, Draw.width(c, l) + lb + rb);
        int h = lines.size() * 10;
        int px = Math.round(x / sc), py = Math.round(y / sc);
        Draw.push(ctx, sc);
        if (s("background").b) {
            int a = Math.max(0, Math.min(255, (int) Math.round(s("bg_opacity").n * 255)));
            ctx.fill(px - 2, py - 2, px + w + 2, py + h, (a << 24) | (s("bg_color").color & 0xFFFFFF));
        }
        if (s("border").b) {
            int t = Math.max(1, (int) s("border_thickness").n), col = s("border_color").color;
            int x0 = px - 2, y0 = py - 2, x1 = px + w + 2, y1 = py + h;
            ctx.fill(x0 - t, y0 - t, x1 + t, y0, col);
            ctx.fill(x0 - t, y1, x1 + t, y1 + t, col);
            ctx.fill(x0 - t, y0, x0, y1, col);
            ctx.fill(x1, y0, x1 + t, y1, col);
        }
        for (int i = 0; i < lines.size(); i++) {
            int ly = py + i * 10, lx = px;
            int col = colors != null && i < colors.length && colors[i] != 0 ? colors[i] : s("text_color").color;
            if (br) { Draw.text(ctx, c, "[", lx, ly, s("bracket_color").color, shadow); lx += lb; }
            Draw.text(ctx, c, lines.get(i), lx, ly, col, shadow);
            if (br) Draw.text(ctx, c, "]", lx + Draw.width(c, lines.get(i)), ly, s("bracket_color").color, shadow);
        }
        Draw.pop(ctx);
    }
}