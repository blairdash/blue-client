package com.blairdash.blueclient.features;

import com.blairdash.blueclient.BlueClientInit;
import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.mods.LegacyOptions;
import net.minecraft.client.MinecraftClient;

/**
 * Smoothly interpolates a render-time FOV override while zoom is active (key held, or toggled).
 * ZoomFovMixin reads getZoomFovOverride() and substitutes it into GameRenderer's FOV computation,
 * which sidesteps the option's 30-110 floor and never touches the real setting.
 */
public class ZoomFeature {
    private static double currentZoomFov = -1; // -1 = not zoomed
    private static boolean wasZooming = false;
    private static boolean prevPressed = false;
    private static boolean toggled = false;

    public static boolean isZooming() {
        return ModConfig.zoomEnabled && wasZooming;
    }

    public static void tick(MinecraftClient client) {
        if (!ModConfig.zoomEnabled || client.player == null) {
            wasZooming = false;
            prevPressed = false;
            toggled = false;
            currentZoomFov = -1;
            return;
        }

        boolean pressed = keyDown(client, BlueClientInit.zoomKey);
        boolean zooming;
        if (LegacyOptions.s("zoom_opts", "toggle").b) {
            if (pressed && !prevPressed && client.currentScreen == null) toggled = !toggled;
            zooming = toggled;
        } else {
            toggled = false;
            zooming = pressed;
        }
        prevPressed = pressed;

        if (zooming) {
            double from = currentZoomFov < 0 ? client.options.getFov().getValue() : currentZoomFov;
            currentZoomFov = lerp(from, LegacyOptions.s("zoom_opts", "fov").n, LegacyOptions.s("zoom_opts", "speed").n);
        } else if (wasZooming) {
            currentZoomFov = -1;
        }
        wasZooming = zooming;
    }

    /**
     * True while the zoom key is physically held. Vanilla only delivers a key to ONE of the bindings
     * that share it, so a conflicting keybind would swallow Zoom; polling the key directly makes Zoom
     * work no matter what else is bound to the same key.
     */
    private static boolean keyDown(MinecraftClient client, net.minecraft.client.option.KeyBinding kb) {
        if (kb.isPressed()) return true;
        if (client.currentScreen != null) return false;
        try {
            net.minecraft.client.util.InputUtil.Key k = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(kb);
            int code = k.getCode();
            if (code < 0) return false;
            long w = client.getWindow().getHandle();
            if (k.getCategory() == net.minecraft.client.util.InputUtil.Type.MOUSE) return org.lwjgl.glfw.GLFW.glfwGetMouseButton(w, code) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            if (k.getCategory() == net.minecraft.client.util.InputUtil.Type.KEYSYM) return org.lwjgl.glfw.GLFW.glfwGetKey(w, code) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } catch (Throwable ignored) { }
        return false;
    }

    public static double getZoomFovOverride() {
        return currentZoomFov;
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
}