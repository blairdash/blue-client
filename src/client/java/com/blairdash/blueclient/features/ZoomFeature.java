package com.blairdash.blueclient.features;

import com.blairdash.blueclient.BlueClientInit;
import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;

/**
 * Smoothly interpolates FOV down while the zoom key is held, and back up
 * when released. Hooking client.options.getFov() every tick is the
 * simplest approach that works without mixins into the render pipeline.
 */
public class ZoomFeature {
    private static double currentFovOverride = -1; // -1 = not zoomed
    private static double normalFov;
    private static boolean wasZooming = false;

    public static void tick(MinecraftClient client) {
        if (!ModConfig.zoomEnabled || client.player == null) {
            return;
        }
        boolean zooming = BlueClientInit.zoomKey.isPressed();
        SimpleOption<Integer> fovOption = client.options.getFov();

        if (zooming && !wasZooming) {
            normalFov = fovOption.getValue();
        }

        if (zooming) {
            currentFovOverride = lerp(currentFovOverride < 0 ? normalFov : currentFovOverride,
                    ModConfig.zoomFov, 0.5);
            fovOption.setValue((int) Math.round(currentFovOverride));
        } else if (wasZooming) {
            fovOption.setValue((int) normalFov);
            currentFovOverride = -1;
        }
        wasZooming = zooming;
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
}
