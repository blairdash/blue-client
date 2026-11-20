package com.blairdash.blueclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Player-head avatars for the account chip. Downloads a 64px face per account once and caches the texture. */
final class Heads {
    private static final Map<String, ResourceLocation> CACHE = new HashMap<String, ResourceLocation>();

    private Heads() {}

    private static ResourceLocation get(String uuidOrName) {
        String key = uuidOrName == null ? "steve" : uuidOrName.replace("-", "").toLowerCase();
        ResourceLocation rl = CACHE.get(key);
        if (rl == null) {
            rl = new ResourceLocation("blueclient", "heads/" + key.replaceAll("[^a-z0-9_.-]", "_"));
            try {
                ITextureObject t = new ThreadDownloadImageData(null, "https://minotar.net/avatar/" + key + "/64.png",
                        DefaultPlayerSkin.getDefaultSkinLegacy(), null);
                Minecraft.getMinecraft().getTextureManager().loadTexture(rl, t);
            } catch (Throwable ignored) {
                rl = DefaultPlayerSkin.getDefaultSkinLegacy();
            }
            CACHE.put(key, rl);
        }
        return rl;
    }

    /** Draws the face at (x, y) in UI units, size x size. */
    static void draw(String uuidOrName, int x, int y, int size) {
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(get(uuidOrName));
        Gui.drawScaledCustomSizeModalRect(x, y, 0f, 0f, 64, 64, size, size, 64f, 64f);
    }
}