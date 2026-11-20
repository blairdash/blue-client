package com.blairdash.blueclient.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Port of the 1.21.x vanilla menu background blur (post_effect/blur.json + post/box_blur.fsh):
 * three iterations of a horizontal + vertical box blur at full resolution with bilinear sampling,
 * radius = menuBackgroundBlurriness (default 5).
 */
public final class MenuBlur {
    private static final String VSH = "#version 150\n"
        + "out vec2 texCoord;\n"
        + "void main(){ vec2 uv = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);\n"
        + "  gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0); texCoord = uv; }\n";
    private static final String FSH = "#version 150\n"
        + "uniform sampler2D InSampler; uniform vec2 InSize; uniform vec2 BlurDir; uniform float Radius;\n"
        + "in vec2 texCoord; out vec4 fragColor;\n"
        + "void main(){ vec2 stepv = (1.0 / InSize) * BlurDir; vec4 blurred = vec4(0.0);\n"
        + "  for (float a = -Radius + 0.5; a <= Radius; a += 2.0) blurred += texture(InSampler, texCoord + stepv * a);\n"
        + "  blurred += texture(InSampler, texCoord + stepv * Radius) / 2.0;\n"
        + "  fragColor = blurred / (Radius + 0.5); }\n";

    private static int prog = 0, vao = 0, uIn, uSize, uDir, uRad;
    private static boolean failed;
    private static SimpleFramebuffer a, b;

    private MenuBlur() {}

    private static int compile(int type, String src) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, src);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL20.GL_COMPILE_STATUS) == 0) throw new IllegalStateException(GL20.glGetShaderInfoLog(s, 2048));
        return s;
    }

    private static void init() {
        try {
            int v = compile(GL20.GL_VERTEX_SHADER, VSH), f = compile(GL20.GL_FRAGMENT_SHADER, FSH);
            prog = GL20.glCreateProgram();
            GL20.glAttachShader(prog, v);
            GL20.glAttachShader(prog, f);
            GL20.glLinkProgram(prog);
            if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == 0) throw new IllegalStateException(GL20.glGetProgramInfoLog(prog, 2048));
            GL20.glDeleteShader(v);
            GL20.glDeleteShader(f);
            uIn = GL20.glGetUniformLocation(prog, "InSampler");
            uSize = GL20.glGetUniformLocation(prog, "InSize");
            uDir = GL20.glGetUniformLocation(prog, "BlurDir");
            uRad = GL20.glGetUniformLocation(prog, "Radius");
            vao = GL30.glGenVertexArrays();
        } catch (Throwable t) {
            failed = true;
            System.err.println("[Blue] menu blur disabled: " + t);
        }
    }

    private static void pass(int srcTex, int sw, int sh, Framebuffer dst, float dx, float dy, int radius) {
        dst.beginWrite(true);
        GlStateManager._bindTexture(srcTex);
        GL20.glUniform2f(uSize, sw, sh);
        GL20.glUniform2f(uDir, dx, dy);
        GL20.glUniform1f(uRad, radius);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    public static void apply(MinecraftClient mc, int radius) {
        if (failed || radius < 1) return;
        if (prog == 0) init();
        if (failed) return;
        Framebuffer main = mc.getFramebuffer();
        int w = main.textureWidth, h = main.textureHeight;
        if (a == null || a.textureWidth != w || a.textureHeight != h) {
            if (a != null) { a.delete(); b.delete(); }
            a = new SimpleFramebuffer(w, h, false, MinecraftClient.IS_SYSTEM_MAC);
            b = new SimpleFramebuffer(w, h, false, MinecraftClient.IS_SYSTEM_MAC);
            a.setTexFilter(GL11.GL_LINEAR);
            b.setTexFilter(GL11.GL_LINEAR);
        }
        int prevProg = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int prevActive = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND), wasDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
            wasCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        main.setTexFilter(GL11.GL_LINEAR);
        GlStateManager._glUseProgram(prog);
        GlStateManager._glBindVertexArray(vao);
        GL20.glUniform1i(uIn, 0);

        int mt = main.getColorAttachment(), at = a.getColorAttachment(), bt = b.getColorAttachment();
        pass(mt, w, h, a, 1f, 0f, radius);
        pass(at, w, h, b, 0f, 1f, radius);
        pass(bt, w, h, a, 1f, 0f, radius);
        pass(at, w, h, b, 0f, 1f, radius);
        pass(bt, w, h, a, 1f, 0f, radius);
        pass(at, w, h, main, 0f, 1f, radius);

        main.setTexFilter(GL11.GL_NEAREST);
        GlStateManager._glBindVertexArray(prevVao);
        GlStateManager._glUseProgram(prevProg);
        GlStateManager._bindTexture(prevTex);
        GlStateManager._activeTexture(prevActive);
        if (wasBlend) RenderSystem.enableBlend();
        if (wasDepth) RenderSystem.enableDepthTest();
        if (wasCull) RenderSystem.enableCull();
        main.beginWrite(true);
    }
}