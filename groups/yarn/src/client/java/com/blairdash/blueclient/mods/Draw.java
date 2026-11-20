package com.blairdash.blueclient.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Small drawing shims so the shared mod code reads the same on every Minecraft version. */
public final class Draw {
    private Draw() { }
    public static void shadow(DrawContext ctx, TextRenderer f, String s, int x, int y, int color) { ctx.drawTextWithShadow(f, s, x, y, color); }
    public static void plain(DrawContext ctx, TextRenderer f, String s, int x, int y, int color, boolean shadow) { ctx.drawText(f, s, x, y, color, shadow); }
    public static void text(DrawContext ctx, MinecraftClient c, String s, int x, int y, int color, boolean shadow) { ctx.drawText(c.textRenderer, s, x, y, color, shadow); }
    public static int width(MinecraftClient c, String s) { return c.textRenderer.getWidth(s); }
    public static int guiW(DrawContext ctx) { return ctx.getScaledWindowWidth(); }
    public static int guiH(DrawContext ctx) { return ctx.getScaledWindowHeight(); }
    public static void push(DrawContext ctx, float s) { ctx.getMatrices().pushMatrix(); ctx.getMatrices().scale(s, s); }
    public static void pop(DrawContext ctx) { ctx.getMatrices().popMatrix(); }
}