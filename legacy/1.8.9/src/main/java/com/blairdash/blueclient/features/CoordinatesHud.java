package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Shows the player's block coordinates. With no position set (-1) it sits in
 * the top-right corner, right-aligned. With a position set, X/Y is the
 * top-left of the text.
 */
public class CoordinatesHud {
    private static String text(Minecraft mc) {
        BlockPos pos = mc.thePlayer.getPosition();
        return String.format("X: %d  Y: %d  Z: %d", pos.getX(), pos.getY(), pos.getZ());
    }

    public static int effectiveX(Minecraft mc, int scaledWidth) {
        if (ModConfig.coordsX >= 0) return ModConfig.coordsX;
        String sample = mc.thePlayer != null ? text(mc) : "X: 0  Y: 0  Z: 0";
        return scaledWidth - mc.fontRendererObj.getStringWidth(sample) - 6;
    }

    public static int effectiveY() {
        return ModConfig.coordsY >= 0 ? ModConfig.coordsY : 6;
    }

    @SubscribeEvent
    public void onRenderPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!ModConfig.coordinatesHudEnabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;

        String text = text(mc);
        int x = effectiveX(mc, event.resolution.getScaledWidth());
        int y = effectiveY();
        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFFFFFFF);
    }
}
