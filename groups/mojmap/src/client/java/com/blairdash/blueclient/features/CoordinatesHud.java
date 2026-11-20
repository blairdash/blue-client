package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;

/**
 * Shows the player's block coordinates. With no position set (-1) it sits in
 * the top-right corner, right-aligned. With a position set, X/Y is the
 * top-left of the text.
 */
public class CoordinatesHud {
    private static String text(Minecraft client) {
        BlockPos pos = client.player.blockPosition();
        return String.format("X: %d  Y: %d  Z: %d", pos.getX(), pos.getY(), pos.getZ());
    }

    /** The X actually used: the configured value, or the top-right default. */
    public static int effectiveX(Minecraft client, int scaledWidth) {
        if (ModConfig.coordsX >= 0) return ModConfig.coordsX;
        String sample = client.player != null ? text(client) : "X: 0  Y: 0  Z: 0";
        return scaledWidth - HudStyle.width(client, sample) - 6;
    }

    public static int effectiveY() {
        return ModConfig.coordsY >= 0 ? ModConfig.coordsY : 6;
    }

    public static void render(GuiGraphicsExtractor ctx, Minecraft client) {
        if (client.player == null) return;
        String text = text(client);
        int x = effectiveX(client, ctx.guiWidth());
        int y = effectiveY();
        HudStyle.line(ctx, client, text, x, y);
    }
}