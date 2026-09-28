package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

/**
 * Shows the player's block coordinates. With no position set (-1) it sits in
 * the top-right corner, right-aligned. With a position set, X/Y is the
 * top-left of the text.
 */
public class CoordinatesHud {
    private static String text(MinecraftClient client) {
        BlockPos pos = client.player.getBlockPos();
        return String.format("X: %d  Y: %d  Z: %d", pos.getX(), pos.getY(), pos.getZ());
    }

    /** The X actually used: the configured value, or the top-right default. */
    public static int effectiveX(MinecraftClient client, int scaledWidth) {
        if (ModConfig.coordsX >= 0) return ModConfig.coordsX;
        String sample = client.player != null ? text(client) : "X: 0  Y: 0  Z: 0";
        return scaledWidth - client.textRenderer.getWidth(sample) - 6;
    }

    public static int effectiveY() {
        return ModConfig.coordsY >= 0 ? ModConfig.coordsY : 6;
    }

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (client.player == null) return;
        String text = text(client);
        int x = effectiveX(client, ctx.getScaledWindowWidth());
        int y = effectiveY();
        ctx.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFFFF);
    }
}