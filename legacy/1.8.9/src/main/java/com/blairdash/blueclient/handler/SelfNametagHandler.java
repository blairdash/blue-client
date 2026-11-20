package com.blairdash.blueclient.handler;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Renders the local player's own name tag above their head, in every camera
 * mode (first person and both third-person views).
 *
 * Vanilla's own per-entity name-tag draw (RendererLivingEntity#canRenderName)
 * always returns false for the entity == RenderManager#livingPlayer (i.e.
 * yourself), so nothing ever draws your own tag through the normal pipeline
 * in ANY camera mode -- first person just additionally skips dispatching the
 * local player through that pipeline at all. Rather than patching vanilla's
 * gate (tried via a Mixin on canRenderName; unreliable in practice), this
 * handler reimplements the same billboard-label draw as a single standalone
 * pass that covers every camera mode itself, so there's exactly one code
 * path to reason about and test.
 *
 * The position math is camera-mode-agnostic: RenderWorldLastEvent fires with
 * the world's modelview matrix already set up for whatever camera the
 * current mode uses (including the third-person pullback, which
 * EntityRenderer#orientCamera applies upstream via its own GL transform), so
 * translating by the player's position relative to
 * TileEntityRendererDispatcher's cached render-origin -- exactly what every
 * other entity's own name tag is drawn relative to -- lands correctly
 * whether the camera is at the player's eyes or pulled back behind/in front
 * of them.
 *
 * The rotation math mirrors RenderManager#playerViewY/X, which in 1.8.9
 * always just mirrors the local player's own view angles (there's no
 * separate free-orbit camera): confirmed via bytecode that
 * RenderManager#cacheActiveRenderInfo adds +180 degrees to that yaw
 * specifically for the F5-front view (thirdPersonView == 2), which is
 * replicated below rather than depending on that cache (which isn't
 * guaranteed to be populated yet on an early frame -- see the NPE this
 * class used to throw for that reason).
 */
public class SelfNametagHandler {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!ModConfig.selfNametagEnabled) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || player.isInvisible() || player.isSpectator()) {
            return;
        }

        float partialTicks = event.partialTicks;
        double interpX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double interpY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double interpZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        double relX = interpX - TileEntityRendererDispatcher.staticPlayerX;
        double relY = interpY - TileEntityRendererDispatcher.staticPlayerY;
        double relZ = interpZ - TileEntityRendererDispatcher.staticPlayerZ;

        float viewYaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;

        // F5 front view mirrors the camera, same as vanilla's own
        // RenderManager#cacheActiveRenderInfo does for playerViewY.
        if (mc.gameSettings.thirdPersonView == 2) {
            viewYaw += 180.0F;
        }

        String name = player.getDisplayName().getUnformattedText();
        // In third person the tag has to tilt with the camera like vanilla's (otherwise, looking down at it from
        // the front view, it is a thin strip). In first person it stays upright so it is readable when you look up.
        float viewPitch = 0F;
        if (mc.gameSettings.thirdPersonView != 0) {
            viewPitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
            if (mc.gameSettings.thirdPersonView == 2) viewPitch = -viewPitch;
        }
        renderNameTag(mc, name, relX, relY + player.height + 0.5D - (player.isSneaking() ? 0.25D : 0D), relZ, viewYaw, viewPitch);
    }

    private void renderNameTag(Minecraft mc, String name, double x, double y, double z, float viewYaw, float viewPitch) {
        FontRenderer fontRenderer = mc.fontRendererObj;
        if (fontRenderer == null) {
            return;
        }
        float scale = 0.016666668F * 1.6F;

        GlStateManager.pushMatrix();
        final boolean wasLit = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GlStateManager.translate((float) x, (float) y, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        // Yaw-only billboard: face the camera left/right but stay upright,
        // rather than also tilting with camera pitch (vanilla's own nametag
        // math does tilt with pitch too, which is fine at a distance but
        // renders edge-on and unreadable when looking straight up/down at
        // your own tag, which sits right above your face).
        GlStateManager.rotate(-viewYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(viewPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        Integer rank = com.blairdash.blueclient.net.RankService.selfColor();
        int slot = rank == null ? 0 : RankNametags.ICON_SLOT;
        int nameW = fontRenderer.getStringWidth(name);
        int total = nameW + slot;
        int left = -total / 2;               // left edge of icon + name
        int nameX = left + slot;

        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(left - 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(left - 1, fontRenderer.FONT_HEIGHT - 1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(left + total + 1, fontRenderer.FONT_HEIGHT - 1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(left + total + 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();

        if (rank != null) RankNametags.drawIcon(mc, rank, 0.27F, left, 0);
        fontRenderer.drawString(name, nameX, 0, 553648127);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        if (rank != null) RankNametags.drawIcon(mc, rank, 1F, left, 0);
        fontRenderer.drawString(name, nameX, 0, -1);

        if (wasLit) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
