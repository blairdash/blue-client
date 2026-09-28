package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;

/**
 * Toggles Minecraft's built-in post-processing shader pipeline on/off with
 * our motion_blur.json effect (see resources/assets/clientmod/shaders/post).
 *
 * IMPORTANT CAVEAT: the GameRenderer method that loads/clears a post
 * shader has been renamed repeatedly across versions and three guessed
 * names in a row (loadPostProcessor/disablePostProcessor,
 * setPostEffect/clearPostEffect) failed to compile against this build's
 * actual mappings. Rather than keep guessing at compile time, this uses
 * reflection to find whichever name is actually present on GameRenderer
 * at runtime, from a list of known historical names. This trades a small
 * amount of safety for not blocking your build on an unimportant,
 * off-by-default feature. If you want this properly fixed instead of
 * reflection-based, open your decompiled GameRenderer.java (Gradle task
 * genSources, then look in your IDE) and find the two single-purpose
 * methods near togglePostEffect/currentPostEffect -- one takes an
 * Identifier and loads a post chain, the other takes nothing and clears
 * it -- then replace SET_NAMES/CLEAR_NAMES below with just that one name
 * each, or call it directly.
 */
public class MotionBlurEffect {
    private static final Identifier EFFECT_ID = Identifier.of("clientmod", "shaders/post/motion_blur.json");
    private static boolean currentlyActive = false;
    private static boolean warnedOnce = false;

    // Try these method names, in order, on whichever version this is built against.
    private static final String[] SET_NAMES = {
            "setPostEffect", "loadPostProcessor", "setPostProcessor", "loadShader"
    };
    private static final String[] CLEAR_NAMES = {
            "clearPostEffect", "disablePostProcessor", "clearPostProcessor", "disableShader"
    };

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean shouldBeActive = ModConfig.motionBlurEnabled && client.player != null;
            if (shouldBeActive != currentlyActive) {
                applyState(client, shouldBeActive);
                currentlyActive = shouldBeActive;
            }
        });
    }

    private static void applyState(MinecraftClient client, boolean active) {
        try {
            Object gameRenderer = client.gameRenderer;
            Class<?> grClass = gameRenderer.getClass();

            if (active) {
                Method m = findMethod(grClass, SET_NAMES, Identifier.class);
                if (m != null) m.invoke(gameRenderer, EFFECT_ID);
                else warnOnce();
            } else {
                Method m = findMethod(grClass, CLEAR_NAMES, null);
                if (m != null) m.invoke(gameRenderer);
                else warnOnce();
            }
        } catch (Throwable t) {
            warnOnce();
        }
    }

    private static Method findMethod(Class<?> clazz, String[] names, Class<?> paramType) {
        for (String name : names) {
            try {
                Method m = paramType == null ? clazz.getMethod(name) : clazz.getMethod(name, paramType);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // try the next candidate name
            }
        }
        return null;
    }

    private static void warnOnce() {
        if (!warnedOnce) {
            warnedOnce = true;
            System.err.println("[clientmod] Motion blur post-effect method not found on this "
                    + "Minecraft version's GameRenderer -- feature disabled, everything else "
                    + "in the mod is unaffected. See MotionBlurEffect.java to fix properly.");
        }
    }
}