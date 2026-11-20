package com.blairdash.blueclient.features;

import com.blairdash.blueclient.BlueClientInit;
import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.mods.LegacyOptions;
import net.minecraft.client.Minecraft;

/**
 * Smoothly interpolates a rendered-FOV override while the zoom key is
 * held, applied at render time by CameraMixin.
 *
 * This deliberately does NOT touch Minecraft's own FOV *setting*
 * (client.options.fov()): vanilla clamps that option to [30, 110]
 * ("options.fov", see Options' construction), and OptionInstance#set
 * silently falls back to the option's *default* (70) -- not the
 * previous value -- whenever a value fails that check. zoomFov is
 * intentionally set below 30 for a real zoom, so every tick's set()
 * was being rejected and reset to 70, which looked like the zoom
 * "snapping back" instantly. Overriding the render-time value in
 * CameraMixin instead sidesteps that clamp entirely and never risks
 * leaving the player's real FOV setting altered.
 */
public class ZoomFeature {
    private static double current = -1; // -1 = not currently overriding
    private static boolean wasZooming = false;
    private static boolean prevPressed = false;
    private static boolean toggled = false;

    public static boolean isZooming() {
        return ModConfig.zoomEnabled && wasZooming;
    }

    public static void tick(Minecraft client) {
        if (!ModConfig.zoomEnabled || client.player == null) {
            wasZooming = false;
            prevPressed = false;
            toggled = false;
            current = -1;
            return;
        }
        boolean pressed = keyDown(client, BlueClientInit.zoomKey);
        boolean zooming;
        if (LegacyOptions.s("zoom_opts", "toggle").b) {
            if (pressed && !prevPressed && client.screen == null) toggled = !toggled;
            zooming = toggled;
        } else {
            toggled = false;
            zooming = pressed;
        }
        prevPressed = pressed;
        if (zooming) {
            double base = current < 0 ? client.options.fov().get() : current;
            current = lerp(base, LegacyOptions.s("zoom_opts", "fov").n, LegacyOptions.s("zoom_opts", "speed").n);
        } else if (wasZooming) {
            current = -1;
        }
        wasZooming = zooming;
    }

    /** The FOV to render at right now, or -1 if zoom isn't active (use the normal value). */
    public static double currentOverride() {
        return current;
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    /**
     * True while the zoom key is physically held. Vanilla only delivers a key to ONE of the bindings
     * that share it, so a conflicting keybind would swallow Zoom; polling the key directly makes Zoom
     * work no matter what else is bound to the same key.
     */
    private static boolean keyDown(Minecraft client, net.minecraft.client.KeyMapping kb) {
        if (kb.isDown()) return true;
return false;
    }
}
