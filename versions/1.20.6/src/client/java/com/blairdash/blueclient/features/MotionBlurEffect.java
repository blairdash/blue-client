package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.lang.reflect.Method;

/**
 * Toggles Minecraft's built-in post-processing shader pipeline on/off with
 * our motion_blur.json post-effect pipeline (see resources/assets/clientmod/post_effect),
 * which reuses vanilla's own box_blur shader program.
 *
 * This only turns the pipeline on while the camera is actually turning
 * quickly (a real "motion" blur), rather than whenever the setting is
 * enabled -- it used to just stay fully blurred the entire time the
 * toggle was on, even standing still. Each tick we measure how far the
 * player's look direction moved since last tick, run that through a
 * decaying peak-hold so the blur doesn't flicker on/off every tick, and
 * only keep the pipeline active while that smoothed value is above a
 * small deadzone. It also engages on fast movement (sprinting/knockback/
 * elytra), not just camera turns, using horizontal speed the same way.
 *
 * GameRenderer.setPostProcessor(Identifier) and .clearPostProcessor() are
 * confirmed (checked directly against this build's own Yarn-mapped
 * Minecraft jar) to be the real, unchanged method names in 1.21.4 -- the
 * "method not found" warning wasn't a naming problem, it was that
 * setPostProcessor is *private*, and the old lookup used
 * Class#getMethod, which only ever returns public methods. Using
 * getDeclaredMethod (which does see private/protected members of the
 * class itself) plus setAccessible finds it correctly.
 */
public class MotionBlurEffect {
    private static final Identifier EFFECT_ID = Identifier.of("clientmod", "motion_blur");

    // Degrees/tick of combined yaw+pitch movement, or blocks/tick of
    // horizontal speed (scaled up to roughly the same range), needed to
    // register as "in motion" at all.
    private static final double TURN_DEADZONE = 1.5;
    private static final double SPEED_DEADZONE = 0.12;
    private static final double SPEED_TO_DEGREES = 40.0;
    // How fast the smoothed motion value decays back toward zero each
    // tick once you stop turning/moving (0.8 = ~4-5 ticks, a quarter
    // second, to fully let go -- long enough not to flicker, short
    // enough to feel responsive).
    private static final double DECAY = 0.8;

    private static boolean currentlyActive = false;
    private static boolean warnedOnce = false;
    private static double smoothedMotion = 0.0;
    private static Float lastYaw = null;
    private static Float lastPitch = null;

    private static Method setPostProcessorMethod;
    private static Method clearPostProcessorMethod;

    static {
        try {
            setPostProcessorMethod = GameRenderer.class.getDeclaredMethod("setPostProcessor", Identifier.class);
            setPostProcessorMethod.setAccessible(true);
            clearPostProcessorMethod = GameRenderer.class.getDeclaredMethod("clearPostProcessor");
            clearPostProcessorMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            warnOnce();
        }
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            double motionThisTick = measureMotion(client);
            smoothedMotion = Math.max(motionThisTick, smoothedMotion * DECAY);

            boolean shouldBeActive = ModConfig.motionBlurEnabled
                    && client.player != null
                    && smoothedMotion > TURN_DEADZONE;
            if (shouldBeActive != currentlyActive) {
                applyState(client, shouldBeActive);
                currentlyActive = shouldBeActive;
            }
        });
    }

    /** Combined camera-turn + movement-speed signal for this tick, in the same rough units as TURN_DEADZONE. */
    private static double measureMotion(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            lastYaw = null;
            lastPitch = null;
            return 0.0;
        }

        float yaw = player.getYaw();
        float pitch = player.getPitch();
        double turn = 0.0;
        if (lastYaw != null) {
            turn = Math.abs(MathHelper.wrapDegrees(yaw - lastYaw)) + Math.abs(pitch - lastPitch);
        }
        lastYaw = yaw;
        lastPitch = pitch;

        double dx = player.getX() - player.prevX;
        double dz = player.getZ() - player.prevZ;
        double horizontalSpeed = Math.sqrt(dx * dx + dz * dz);
        double speedSignal = horizontalSpeed > SPEED_DEADZONE ? horizontalSpeed * SPEED_TO_DEGREES : 0.0;

        return Math.max(turn, speedSignal);
    }

    private static void applyState(MinecraftClient client, boolean active) {
        try {
            if (active) {
                if (setPostProcessorMethod != null) setPostProcessorMethod.invoke(client.gameRenderer, EFFECT_ID);
                else warnOnce();
            } else {
                if (clearPostProcessorMethod != null) clearPostProcessorMethod.invoke(client.gameRenderer);
                else warnOnce();
            }
        } catch (Throwable t) {
            warnOnce();
        }
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
