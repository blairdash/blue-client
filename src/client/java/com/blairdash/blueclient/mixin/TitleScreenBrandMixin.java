package com.blairdash.blueclient.mixin;

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Replaces the bottom-left title screen text
 * ("Minecraft 1.21.11/Fabric (Modded)") with the client's own branding.
 * TitleScreen.render builds that text as a String and draws it with
 * DrawContext.drawTextWithShadow(TextRenderer, String, int, int, int),
 * so we swap the String argument (index 1) right before the draw.
 */
@Mixin(TitleScreen.class)
public class TitleScreenBrandMixin {

    private static final String BRAND_TEXT = "Blue Client V1";

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"),
            index = 1)
    private String clientmod$brandText(String original) {
        return BRAND_TEXT;
    }
}