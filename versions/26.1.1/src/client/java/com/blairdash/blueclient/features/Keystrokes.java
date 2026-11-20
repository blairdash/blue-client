package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.mods.Draw;
import com.blairdash.blueclient.mods.LegacyOptions;
import com.blairdash.blueclient.mods.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shows W / A / S / D, optionally the mouse buttons (with CPS) and the spacebar. A key lights up
 * while it's held. Uses the game's own key bindings, so remapped keys work.
 */
public class Keystrokes {
    private static final int KEY = 22;    // key size in scaled GUI pixels
    private static final int GAP = 2;
    private static final int SPACE_H = 12;
    private static final int DEFAULT_X = 6;

    private static Setting s(String id) { return LegacyOptions.s("keystrokes_opts", id); }
    private static float sc() { return (float) s("scale").n; }

    private static int rawWidth() { return 3 * KEY + 2 * GAP; }

    private static int rawHeight() {
        int h = 2 * KEY + GAP;
        if (s("mouse").b) h += GAP + KEY;
        if (s("space").b) h += GAP + SPACE_H;
        return h;
    }

    public static int width() { return Math.round(rawWidth() * sc()); }

    public static int height() { return Math.round(rawHeight() * sc()); }

    /** The X actually used: the configured value, or the left edge. */
    public static int effectiveX() {
        return ModConfig.keystrokesX >= 0 ? ModConfig.keystrokesX : DEFAULT_X;
    }

    /** The Y actually used: the configured value, or vertically centered. */
    public static int effectiveY(int scaledHeight) {
        return ModConfig.keystrokesY >= 0 ? ModConfig.keystrokesY : (scaledHeight - height()) / 2;
    }

    private static int argb(String color, String opacity) {
        int a = Math.max(0, Math.min(255, (int) Math.round(s(opacity).n * 255)));
        return (a << 24) | (s(color).color & 0xFFFFFF);
    }

    public static void render(GuiGraphicsExtractor ctx, Minecraft client) {
        if (client.options.hideGui) return;

        float k = sc();
        int x = Math.round(effectiveX() / k);
        int y = Math.round(effectiveY(ctx.guiHeight()) / k);
        int step = KEY + GAP;
        Draw.push(ctx, k);

        // Row 1: W
        drawKey(ctx, client, x + step, y, KEY, KEY, "W", null, client.options.keyUp.isDown());

        // Row 2: A S D
        int y2 = y + step;
        drawKey(ctx, client, x, y2, KEY, KEY, "A", null, client.options.keyLeft.isDown());
        drawKey(ctx, client, x + step, y2, KEY, KEY, "S", null, client.options.keyDown.isDown());
        drawKey(ctx, client, x + 2 * step, y2, KEY, KEY, "D", null, client.options.keyRight.isDown());

        // Row 3: LMB RMB (together exactly as wide as the rows above)
        int next = y2 + step;
        if (s("mouse").b) {
            int mouseWidth = (rawWidth() - GAP) / 2;
            boolean cps = s("cps").b;
            drawKey(ctx, client, x, next, mouseWidth, KEY, "LMB", cps ? String.valueOf(CpsFpsHud.leftCps()) : null, client.options.keyAttack.isDown());
            drawKey(ctx, client, x + mouseWidth + GAP, next, mouseWidth, KEY, "RMB", cps ? String.valueOf(CpsFpsHud.rightCps()) : null, client.options.keyUse.isDown());
            next += KEY + GAP;
        }
        if (s("space").b) {
            drawKey(ctx, client, x, next, rawWidth(), SPACE_H, "SPACE", null, client.options.keyJump.isDown());
        }
        Draw.pop(ctx);
    }

    private static void drawKey(GuiGraphicsExtractor ctx, Minecraft client, int x, int y, int w, int h,
                                String label, String extra, boolean pressed) {
        ctx.fill(x, y, x + w, y + h, pressed ? argb("pressed_color", "pressed_opacity") : argb("bg_color", "bg_opacity"));
        if (s("border").b) {
            int col = s("border_color").color;
            ctx.fill(x, y, x + w, y + 1, col);
            ctx.fill(x, y + h - 1, x + w, y + h, col);
            ctx.fill(x, y, x + 1, y + h, col);
            ctx.fill(x + w - 1, y, x + w, y + h, col);
        }
        int color = pressed ? s("pressed_text_color").color : s("text_color").color;
        int textX = x + (w - Draw.width(client, label)) / 2;
        int textY = extra != null ? y + 3 : y + (h - 8) / 2;
        Draw.text(ctx, client, label, textX, textY, color, !pressed);
        if (extra != null) Draw.text(ctx, client, extra, x + (w - Draw.width(client, extra)) / 2, y + 12, color, false);
    }
}
