package com.blairdash.blueclient.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small drawing shims so the shared mod code reads the same on every Minecraft version. */
public final class Draw {
    private Draw() { }
    public static void shadow(GuiGraphicsExtractor ctx, Font f, String s, int x, int y, int color) { ctx.text(f, s, x, y, color, true); }
    public static void plain(GuiGraphicsExtractor ctx, Font f, String s, int x, int y, int color, boolean shadow) { ctx.text(f, s, x, y, color, shadow); }
    public static void text(GuiGraphicsExtractor ctx, Minecraft c, String s, int x, int y, int color, boolean shadow) { ctx.text(c.font, s, x, y, color, shadow); }
    public static int width(Minecraft c, String s) { return c.font.width(s); }
    public static int guiW(GuiGraphicsExtractor ctx) { return ctx.guiWidth(); }
    public static int guiH(GuiGraphicsExtractor ctx) { return ctx.guiHeight(); }
    public static void push(GuiGraphicsExtractor ctx, float s) { ctx.pose().pushMatrix(); ctx.pose().scale(s, s); }
    public static void pop(GuiGraphicsExtractor ctx) { ctx.pose().popMatrix(); }
}