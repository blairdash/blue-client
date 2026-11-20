package com.blairdash.blueclient.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.GuiPipeline;
import net.minecraft.client.renderer.texture.TextureResources;
import net.minecraft.resources.Identifier;

/** Texture drawing / registration, which differs between Minecraft versions. */
final class GfxCompat {
    private GfxCompat() { }

    record Tex(Identifier id, int w, int h) { }

    /** Draws a whole texture scaled to size x size. */
    static void draw(GuiGraphicsExtractor g, Identifier id, int x, int y, int size, int texW, int texH) {
        g.blit(GuiPipeline.TEXTURED, id, x, y, 0f, 0f, size, size, texW, texH, texW, texH);
    }

    /** Registers a decoded image as a texture. */
    static Tex register(Minecraft mc, Identifier id, NativeImage img, String key) {
        mc.getTextureManager().register(id, TextureResources.from2dImage(() -> key, img));
        return new Tex(id, img.getWidth(), img.getHeight());
    }
}
