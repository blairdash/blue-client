package com.blairdash.blueclient.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rewrites the window title from "Minecraft* 1.21.11" to
 * "Blue Client 1.21.11 (v1)". Whatever vanilla appends after the version
 * (" - Singleplayer", " - Multiplayer (3rd-party Server)") is kept.
 */
@Mixin(MinecraftClient.class)
public class WindowTitleMixin {

    private static final String CLIENT_NAME = "Blue Client";
    private static final String CLIENT_VERSION = "v1";

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void clientmod$customTitle(CallbackInfoReturnable<String> cir) {
        // "Minecraft* 1.21.11 ..." -> "Blue Client 1.21.11 (v1) ..."
        String title = cir.getReturnValue().replaceFirst(
                "^Minecraft\\*?\\s+(\\S+)",
                CLIENT_NAME + " $1 (" + CLIENT_VERSION + ")");
        cir.setReturnValue(title);
    }
}