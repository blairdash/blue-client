package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.BlueClientInit;
import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the zoom key is held, the scroll wheel adjusts the zoom level
 * instead of switching hotbar slots. Scroll up = zoom in, scroll down =
 * zoom out. The level is remembered until you quit the game.
 */
@Mixin(MouseHandler.class)
public class MouseMixin {

    private static final double MIN_ZOOM_FOV = 4.0;   // most zoomed in
    private static final double MAX_ZOOM_FOV = 60.0;  // least zoomed in
    private static final double STEP = 0.9;           // 10% per notch

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void clientmod$scrollZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!ModConfig.zoomEnabled || !com.blairdash.blueclient.features.ZoomFeature.isZooming()
                || !com.blairdash.blueclient.mods.LegacyOptions.s("zoom_opts", "scroll").b) {
            return;
        }
        if (vertical == 0 || Minecraft.getInstance().screen != null) {
            return;
        }

        // vertical > 0 (scroll up) shrinks the FOV = zoom in
        var zoom = com.blairdash.blueclient.mods.LegacyOptions.s("zoom_opts", "fov");
        double fov = zoom.n * Math.pow(STEP, vertical);
        zoom.n = Math.max(MIN_ZOOM_FOV, Math.min(MAX_ZOOM_FOV, fov));

        ci.cancel(); // don't also scroll the hotbar
    }
}
