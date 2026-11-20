package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Draws a simple cross-shaped crosshair whose size/color come from
 * ModConfig, cancelling vanilla's own crosshair first so this doesn't
 * just draw on top of it.
 *
 * Unlike the 26.x branches, there is no "attack strength cooldown"
 * indicator here: that's part of the 1.9+ combat rewrite and simply
 * doesn't exist in 1.8.9 (EntityPlayer has no cooldown-scale method at
 * all), so it's intentionally omitted rather than guessed at.
 */
public class CrosshairOverlay {

    @SubscribeEvent
    public void onRenderPre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        if (!ModConfig.customCrosshairEnabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.hideGUI || mc.thePlayer == null) return;

        event.setCanceled(true); // suppress vanilla's crosshair; we draw our own below
        draw(mc, event.resolution.getScaledWidth(), event.resolution.getScaledHeight());
    }

    private static boolean isLookingAtEntity(Minecraft mc) {
        MovingObjectPosition target = mc.objectMouseOver;
        return target != null && target.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY;
    }

    private static void draw(Minecraft mc, int screenWidth, int screenHeight) {
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;
        int len = (int) (ModConfig.crosshairSize / 2);
        int gap = (int) ModConfig.crosshairGap;
        int thickness = Math.max(1, (int) ModConfig.crosshairThickness);

        boolean onEnemy = ModConfig.enemyCrosshairEnabled && isLookingAtEntity(mc);
        int color = onEnemy ? ModConfig.enemyCrosshairColor : ModConfig.crosshairColor;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Gui.drawRect(cx - gap - len, cy - thickness / 2, cx - gap, cy - thickness / 2 + thickness, color);
        Gui.drawRect(cx + gap, cy - thickness / 2, cx + gap + len, cy - thickness / 2 + thickness, color);
        Gui.drawRect(cx - thickness / 2, cy - gap - len, cx - thickness / 2 + thickness, cy - gap, color);
        Gui.drawRect(cx - thickness / 2, cy + gap, cx - thickness / 2 + thickness, cy + gap + len, color);

        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); // Gui.drawRect leaves the crosshair colour bound; it tinted the hotbar/hearts
    }
}
