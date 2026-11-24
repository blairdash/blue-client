package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Draws the custom crosshair (shapes, outline, dot, pixel crosshair, Friendly / Enemy profiles, bow and swing
 * spread) and cancels vanilla's own crosshair first. There is no attack-cooldown indicator in 1.8.9: that is part
 * of the 1.9+ combat rewrite, so it is left out here.
 */
public class CrosshairOverlay {

    @SubscribeEvent
    public void onRenderPre(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        if (!ModConfig.customCrosshairEnabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.hideGUI || mc.thePlayer == null) return;
        if (mc.gameSettings.thirdPersonView != 0 && !CrosshairData.b("show_f5")) return;

        event.setCanceled(true); // suppress vanilla's crosshair; we draw our own below
        draw(mc, event.resolution.getScaledWidth(), event.resolution.getScaledHeight(), event.partialTicks);
    }

    /** 0 Normal, 1 Friendly (teammate in sight), 2 Enemy (hostile mob or other player in sight). */
    private static int pickProfile(Minecraft mc) {
        MovingObjectPosition t = mc.objectMouseOver;
        if (t == null || t.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY || t.entityHit == null) return 0;
        Entity e = t.entityHit;
        if (e instanceof EntityPlayer && sameTeam(mc, e) && CrosshairData.b("friendly")) return 1;
        if (CrosshairData.b("enemy") && (e instanceof IMob || (e instanceof EntityPlayer && !sameTeam(mc, e)))) return 2;
        return 0;
    }

    private static boolean sameTeam(Minecraft mc, Entity e) {
        net.minecraft.scoreboard.Team mine = mc.thePlayer.getTeam();
        return mine != null && e instanceof EntityPlayer && mine.isSameTeam(((EntityPlayer) e).getTeam());
    }

    private static void draw(Minecraft mc, int screenWidth, int screenHeight, float partialTicks) {
        boolean bow = mc.thePlayer.isUsingItem() && mc.thePlayer.getItemInUse() != null && mc.thePlayer.getItemInUse().getItem() instanceof ItemBow;
        double dyn = CrosshairRenderer.dynamic(bow, bow ? mc.thePlayer.getItemInUseDuration() : 0, mc.thePlayer.getSwingProgress(partialTicks));
        List<int[]> rects = CrosshairRenderer.build(screenWidth / 2, screenHeight / 2, pickProfile(mc), dyn);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        for (int[] r : rects) Gui.drawRect(r[0], r[1], r[2], r[3], r.length > 4 ? r[4] : 0xFFFFFFFF);
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); // Gui.drawRect leaves the crosshair colour bound; it tinted the hotbar/hearts
    }
}