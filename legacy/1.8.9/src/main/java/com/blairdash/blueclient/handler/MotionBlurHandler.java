package com.blairdash.blueclient.handler;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Frame-accumulation motion blur, done entirely with vanilla's own
 * Framebuffer + fixed-function blending -- no custom GLSL/ShaderGroup JSON
 * needed. Every method used here (Framebuffer#bindFramebuffer/
 * unbindFramebuffer/framebufferRenderExt/framebufferClear, GlStateManager's
 * ortho/translate/blend/bindTexture(int), WorldRenderer#pos/tex/endVertex)
 * was confirmed against the real deobfuscated 1.8.9 jar via javap; the
 * fullscreen-quad draw and ortho setup mirror Framebuffer#framebufferRenderExt
 * itself, since that's the one place vanilla already does this exact thing.
 *
 * Technique: keep two off-screen buffers and ping-pong between them each
 * frame. Each frame, draw the freshly-rendered scene (Minecraft's own main
 * framebuffer) fully opaque into the new buffer, then blend last frame's
 * buffer on top at a fading alpha -- the fading old frames are the "blur"
 * trail. The blended result is then copied back onto the main framebuffer
 * so it's what actually gets displayed this frame.
 */
public class MotionBlurHandler {
    private static final int GL_PROJECTION = 5889;
    private static final int GL_MODELVIEW = 5888;
    private static final int GL_NEAREST = 9728;

    private Framebuffer bufferA;
    private Framebuffer bufferB;
    private boolean useAAsSource = true;
    private int bufferWidth;
    private int bufferHeight;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!ModConfig.motionBlurEnabled) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        int width = mc.displayWidth;
        int height = mc.displayHeight;
        if (width <= 0 || height <= 0) {
            return;
        }

        if (bufferA == null || bufferWidth != width || bufferHeight != height) {
            recreateBuffers(width, height);
        }

        Framebuffer main = mc.getFramebuffer();
        Framebuffer src = useAAsSource ? bufferA : bufferB;
        Framebuffer dst = useAAsSource ? bufferB : bufferA;

        // How much of the previous frame survives into this one. 0 = no
        // trail (blur off in all but name), close to 1 = a long smear.
        float trail = Math.max(0.0F, Math.min(0.95F, ModConfig.motionBlurStrength));

        main.unbindFramebuffer();
        dst.bindFramebuffer(true);
        dst.framebufferClear();

        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);

        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);

        // Fresh scene, fully opaque, as the base of this frame's buffer.
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawFullscreenQuad(main.framebufferTexture, width, height,
                main.framebufferTextureWidth, main.framebufferTextureHeight);

        // Fade the previous frame's buffer on top of it.
        if (trail > 0.0F) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA
            GlStateManager.color(1.0F, 1.0F, 1.0F, trail);
            drawFullscreenQuad(src.framebufferTexture, width, height,
                    src.framebufferTextureWidth, src.framebufferTextureHeight);
            GlStateManager.disableBlend();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);

        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL_MODELVIEW);

        dst.unbindFramebuffer();
        main.bindFramebuffer(true);

        // Composite this frame's blended buffer back onto the screen.
        dst.framebufferRenderExt(width, height, true);

        useAAsSource = !useAAsSource;
    }

    private void drawFullscreenQuad(int textureId, int width, int height, int texWidth, int texHeight) {
        GlStateManager.bindTexture(textureId);
        float u = (float) width / (float) texWidth;
        float v = (float) height / (float) texHeight;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos(0.0D, height, 0.0D).tex(0.0D, 0.0D).endVertex();
        wr.pos(width, height, 0.0D).tex(u, 0.0D).endVertex();
        wr.pos(width, 0.0D, 0.0D).tex(u, v).endVertex();
        wr.pos(0.0D, 0.0D, 0.0D).tex(0.0D, v).endVertex();
        tessellator.draw();
    }

    private void recreateBuffers(int width, int height) {
        if (bufferA != null) {
            bufferA.deleteFramebuffer();
        }
        if (bufferB != null) {
            bufferB.deleteFramebuffer();
        }

        bufferA = new Framebuffer(width, height, false);
        bufferA.setFramebufferFilter(GL_NEAREST);
        bufferA.createBindFramebuffer(width, height);
        bufferA.framebufferClear();

        bufferB = new Framebuffer(width, height, false);
        bufferB.setFramebufferFilter(GL_NEAREST);
        bufferB.createBindFramebuffer(width, height);
        bufferB.framebufferClear();

        bufferWidth = width;
        bufferHeight = height;
        useAAsSource = true;

        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
    }
}
