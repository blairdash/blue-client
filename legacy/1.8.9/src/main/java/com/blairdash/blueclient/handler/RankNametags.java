package com.blairdash.blueclient.handler;

import com.blairdash.blueclient.net.RankService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * The Blue Client icon, tinted with the player's rank colour. Drawn to the left of every other online
 * Blue Client user's name tag here, and of your own tag by {@link SelfNametagHandler}.
 */
public class RankNametags {
    public static final ResourceLocation ICON = new ResourceLocation("blueclient", "textures/rank.png");
    /** Width reserved for the icon in front of a name, in name-tag pixels. */
    public static final int ICON_SLOT = 10;

    /** Draws the 8x8 icon at (x, y) in name-tag space (texture must be re-bound by the caller afterwards). */
    public static void drawIcon(Minecraft mc, int argb, float alpha, int x, int y) {
        mc.getTextureManager().bindTexture(ICON);
        GlStateManager.enableTexture2D();
        GlStateManager.color(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, alpha);
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        w.begin(7, DefaultVertexFormats.POSITION_TEX);
        w.pos(x, y, 0).tex(0, 0).endVertex();
        w.pos(x, y + 8, 0).tex(0, 1).endVertex();
        w.pos(x + 8, y + 8, 0).tex(1, 1).endVertex();
        w.pos(x + 8, y, 0).tex(1, 0).endVertex();
        t.draw();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    @SubscribeEvent
    public void onSpecialsPost(RenderLivingEvent.Specials.Post e) {
        if (!(e.entity instanceof EntityPlayer)) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = (EntityPlayer) e.entity;
        EntityPlayerSP me = mc.thePlayer;
        if (me == null || p == me || !Minecraft.isGuiEnabled() || p.isInvisibleToPlayer(me) || p.riddenByEntity != null) return;
        double dist = p.getDistanceSqToEntity(me);
        if (dist > (p.isSneaking() ? 32 * 32 : 64 * 64)) return;
        Integer col = RankService.colorFor(p.getUniqueID());
        if (col == null) return;

        float scale = 0.016666668F * 1.6F;
        int nameW = mc.fontRendererObj.getStringWidth(p.getDisplayName().getFormattedText());
        float yOff = p.height + 0.5F - (p.isSneaking() ? 0.25F : 0F);
        // vanilla shifts the tag up when the player has a scoreboard objective under their name
        if (dist < 100 * 100 && mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(2) != null && !p.isSneaking()) yOff += mc.fontRendererObj.FONT_HEIGHT * scale * 1.15F;

        GlStateManager.pushMatrix();
        final boolean wasLit = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GlStateManager.translate((float) e.x, (float) e.y + yOff, (float) e.z);
        GL11.glNormal3f(0f, 1f, 0f);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0f, 1f, 0f);
        GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * mc.getRenderManager().playerViewX, 1f, 0f, 0f);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int x = -nameW / 2 - ICON_SLOT;
        drawIcon(mc, col, 0.27f, x, 0);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        drawIcon(mc, col, 1f, x, 0);
        if (wasLit) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }
}
