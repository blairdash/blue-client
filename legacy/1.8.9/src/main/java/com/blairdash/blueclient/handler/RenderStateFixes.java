package com.blairdash.blueclient.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Two small render-state fixes:
 *  - the hotbar picked up a gray tint from GL state (lighting / colour) leaked by an earlier overlay pass, so the
 *    state vanilla assumes is forced back to defaults right before the hotbar is drawn;
 *  - in the front-facing third-person view (F5 twice) vanilla's name tags use an un-mirrored camera pitch, so other
 *    players' tags did not turn with the camera. The pitch is mirrored for the duration of the tag draw.
 */
public class RenderStateFixes {
    private boolean flipped;

    @SubscribeEvent
    public void onHotbarPre(RenderGameOverlayEvent.Pre e) {
        if (e.type == RenderGameOverlayEvent.ElementType.ALL || e.type == RenderGameOverlayEvent.ElementType.TEXT) return;
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Chat (timestamps and coloured text) and HUD text leave their last colour bound; clear it after each element. */
    @SubscribeEvent
    public void onOverlayPost(RenderGameOverlayEvent.Post e) {
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpecialsPre(RenderLivingEvent.Specials.Pre e) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager rm = mc.getRenderManager();
        if (flipped) { rm.playerViewX = -rm.playerViewX; flipped = false; }
        if (mc.gameSettings.thirdPersonView == 2) { rm.playerViewX = -rm.playerViewX; flipped = true; }
    }

    /** Runs before the other Post handlers so they see the original pitch again. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpecialsPost(RenderLivingEvent.Specials.Post e) {
        if (!flipped) return;
        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        rm.playerViewX = -rm.playerViewX;
        flipped = false;
    }
}