package com.blairdash.blueclient;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** Replaces the grass-block window/taskbar icon with the Blue Client logo. Version independent (plain GLFW). */
public final class WindowIcon {
    private WindowIcon() { }

    private static boolean done;
    private static final int[] SIZES = {16, 32, 48, 64, 128, 256};

    /** Call from the render thread; the game window is that thread's current GLFW context. */
    public static void apply() {
        if (done) return;
        try {
            long window = GLFW.glfwGetCurrentContext();
            if (window == 0L) return;
            done = true;
            BufferedImage src;
            try (InputStream in = WindowIcon.class.getResourceAsStream("/assets/clientmod/textures/gui/logo.png")) {
                if (in == null) return;
                src = ImageIO.read(in);
            }
            GLFWImage.Buffer images = GLFWImage.malloc(SIZES.length);
            ByteBuffer[] pixelBufs = new ByteBuffer[SIZES.length];
            try {
                for (int i = 0; i < SIZES.length; i++) {
                    int s = SIZES[i];
                    BufferedImage scaled = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = scaled.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g.drawImage(src, 0, 0, s, s, null);
                    g.dispose();
                    ByteBuffer buf = MemoryUtil.memAlloc(s * s * 4);
                    for (int y = 0; y < s; y++) {
                        for (int x = 0; x < s; x++) {
                            int argb = scaled.getRGB(x, y);
                            buf.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >>> 24));
                        }
                    }
                    buf.flip();
                    pixelBufs[i] = buf;
                    images.get(i).set(s, s, buf);
                }
                GLFW.glfwSetWindowIcon(window, images);
            } finally {
                images.free();
                for (ByteBuffer b : pixelBufs) if (b != null) MemoryUtil.memFree(b);
            }
        } catch (Throwable t) {
            done = true;
            System.out.println("[blueclient] could not set window icon: " + t);
        }
    }
}
