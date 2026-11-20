package com.blairdash.blueclient;

import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLPixels;
import org.lwjgl.sdl.SDLSurface;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** Replaces the grass-block window/taskbar icon with the Blue Client logo (26.3+ windows run on SDL, not GLFW). */
public final class WindowIcon {
    private WindowIcon() { }

    private static boolean done;
    private static final int SIZE = 256;

    public static void apply() {
        if (done) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) return;
            long window = mc.getWindow().handle();
            if (window == 0L) return;
            done = true;
            BufferedImage src;
            try (InputStream in = WindowIcon.class.getResourceAsStream("/assets/clientmod/textures/gui/logo.png")) {
                if (in == null) return;
                src = ImageIO.read(in);
            }
            BufferedImage scaled = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(src, 0, 0, SIZE, SIZE, null);
            g.dispose();
            ByteBuffer buf = MemoryUtil.memAlloc(SIZE * SIZE * 4);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int argb = scaled.getRGB(x, y);
                    buf.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >>> 24));
                }
            }
            buf.flip();
            try {
                SDL_Surface surface = SDLSurface.SDL_CreateSurfaceFrom(SIZE, SIZE, SDLPixels.SDL_PIXELFORMAT_RGBA32, buf, SIZE * 4);
                if (surface != null) {
                    SDLVideo.SDL_SetWindowIcon(window, surface); // SDL copies the pixels
                    SDLSurface.SDL_DestroySurface(surface);
                }
            } finally {
                MemoryUtil.memFree(buf);
            }
        } catch (Throwable t) {
            done = true;
            System.out.println("[blueclient] could not set window icon: " + t);
        }
    }
}
