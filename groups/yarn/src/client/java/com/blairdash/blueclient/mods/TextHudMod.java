package com.blairdash.blueclient.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;

import java.util.List;

/** A HUD mod that just shows a few lines of text. Shared style options: scale, brackets, background, border. */
public abstract class TextHudMod extends BlueMod {
    protected final Setting textColor = color("color", "Text color", 0xFFFFFFFF);
    protected final Setting shadow = bool("shadow", "Text shadow", true);
    protected final Setting background = bool("background", "Background", false);
    protected final Setting scale = num("scale", "Scale", 0.5, 3.0, 2, 1.0);
    protected final Setting brackets = bool("brackets", "Brackets", false);
    protected final Setting bracketColor = color("bracket_color", "Bracket color", 0xFFAAAAAA);
    protected final Setting bgColor = color("bg_color", "Background color", 0xFF000000);
    protected final Setting bgOpacity = num("bg_opacity", "Background opacity", 0.0, 1.0, 2, 0.4);
    protected final Setting border = bool("border", "Border", false);
    protected final Setting borderColor = color("border_color", "Border color", 0xFFFFFFFF);
    protected final Setting borderThickness = num("border_thickness", "Border thickness", 1, 3, 0, 1);
    private final String sample;

    protected TextHudMod(String id, String name, String category, Item icon, String sample) {
        super(id, name, category, icon);
        this.sample = sample;
    }

    /** The lines to show right now (empty = draw nothing). */
    protected abstract List<String> lines(MinecraftClient c);

    @Override public boolean isHud() { return true; }

    @Override public int hudW(MinecraftClient c) {
        int w = Math.max(Draw.width(c, sample), 40) + (brackets.b ? Draw.width(c, "[]") : 0);
        return Math.round((float) (w * scale.n));
    }
    @Override public int hudH(MinecraftClient c) { return Math.round((float) (10 * Math.max(1, sampleLines()) * scale.n)); }
    protected int sampleLines() { return sample.split("\n").length; }

    @Override
    public void render(DrawContext ctx, MinecraftClient c) {
        List<String> lines = lines(c);
        if (lines == null || lines.isEmpty()) return;
        float s = (float) scale.n;
        int sw = Draw.guiW(ctx), sh = Draw.guiH(ctx);
        int x = Math.round(x(c, sw, sh) / s), y = Math.round(y(c, sw, sh) / s);
        int lb = brackets.b ? Draw.width(c, "[") : 0, rb = brackets.b ? Draw.width(c, "]") : 0;
        int w = 0;
        for (String l : lines) w = Math.max(w, Draw.width(c, l) + lb + rb);
        int h = lines.size() * 10;
        Draw.push(ctx, s);
        if (background.b) {
            int a = Math.max(0, Math.min(255, (int) Math.round(bgOpacity.n * 255)));
            ctx.fill(x - 2, y - 2, x + w + 2, y + h, (a << 24) | (bgColor.color & 0xFFFFFF));
        }
        if (border.b) {
            int t = Math.max(1, (int) borderThickness.n), col = borderColor.color;
            int x0 = x - 2, y0 = y - 2, x1 = x + w + 2, y1 = y + h;
            ctx.fill(x0 - t, y0 - t, x1 + t, y0, col);
            ctx.fill(x0 - t, y1, x1 + t, y1 + t, col);
            ctx.fill(x0 - t, y0, x0, y1, col);
            ctx.fill(x1, y0, x1 + t, y1, col);
        }
        for (int i = 0; i < lines.size(); i++) {
            int ly = y + i * 10, lx = x;
            if (brackets.b) { Draw.text(ctx, c, "[", lx, ly, bracketColor.color, shadow.b); lx += lb; }
            Draw.text(ctx, c, lines.get(i), lx, ly, textColor.color, shadow.b);
            if (brackets.b) Draw.text(ctx, c, "]", lx + Draw.width(c, lines.get(i)), ly, bracketColor.color, shadow.b);
        }
        Draw.pop(ctx);
    }
}