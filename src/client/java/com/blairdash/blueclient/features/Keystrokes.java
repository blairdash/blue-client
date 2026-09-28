package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Shows W / A / S / D and the left / right mouse buttons. A key lights up
 * while it's held. Uses the game's own key bindings, so remapped keys work.
 *
 *        [ W ]
 *   [ A ] [ S ] [ D ]
 *   [ LMB ] [ RMB ]
 */
public class Keystrokes {
    private static final int KEY = 22;    // key size in scaled GUI pixels
    private static final int GAP = 2;
    private static final int DEFAULT_X = 6;

    private static final int IDLE_BG = 0x80000000;
    private static final int PRESSED_BG = 0xCCFFFFFF;
    private static final int IDLE_TEXT = 0xFFFFFFFF;
    private static final int PRESSED_TEXT = 0xFF000000;

    public static int width() {
        return 3 * KEY + 2 * GAP;
    }

    public static int height() {
        return 3 * KEY + 2 * GAP;
    }

    /** The X actually used: the configured value, or the left edge. */
    public static int effectiveX() {
        return ModConfig.keystrokesX >= 0 ? ModConfig.keystrokesX : DEFAULT_X;
    }

    /** The Y actually used: the configured value, or vertically centered. */
    public static int effectiveY(int scaledHeight) {
        return ModConfig.keystrokesY >= 0 ? ModConfig.keystrokesY : (scaledHeight - height()) / 2;
    }

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (client.options.hudHidden) return;

        int x = effectiveX();
        int y = effectiveY(ctx.getScaledWindowHeight());
        int step = KEY + GAP;

        // Row 1: W
        drawKey(ctx, client, x + step, y, KEY, KEY, "W", client.options.forwardKey.isPressed());

        // Row 2: A S D
        int y2 = y + step;
        drawKey(ctx, client, x, y2, KEY, KEY, "A", client.options.leftKey.isPressed());
        drawKey(ctx, client, x + step, y2, KEY, KEY, "S", client.options.backKey.isPressed());
        drawKey(ctx, client, x + 2 * step, y2, KEY, KEY, "D", client.options.rightKey.isPressed());

        // Row 3: LMB RMB (together exactly as wide as the rows above)
        int y3 = y2 + step;
        int mouseWidth = (width() - GAP) / 2;
        drawKey(ctx, client, x, y3, mouseWidth, KEY, "LMB", client.options.attackKey.isPressed());
        drawKey(ctx, client, x + mouseWidth + GAP, y3, mouseWidth, KEY, "RMB", client.options.useKey.isPressed());
    }

    private static void drawKey(DrawContext ctx, MinecraftClient client, int x, int y, int w, int h,
                                String label, boolean pressed) {
        ctx.fill(x, y, x + w, y + h, pressed ? PRESSED_BG : IDLE_BG);

        int textWidth = client.textRenderer.getWidth(label);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (h - 8) / 2;
        // Dark text on a white key looks smudged with a shadow, so only shadow idle keys.
        ctx.drawText(client.textRenderer, label, textX, textY, pressed ? PRESSED_TEXT : IDLE_TEXT, !pressed);
    }
}