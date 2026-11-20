package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.mods.LegacyOptions;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/** Adapts the 1.8.9 tap/hold sprint and sneak behavior to Fabric's movement bindings. */
public class ToggleMovement {
    private static final long SPRINT_TAP_NANOS = 400_000_000L;
    private static final long SNEAK_TAP_NANOS = 200_000_000L;
    private static final float BASE_FLY_SPEED = 0.05f;
    private static final double VERTICAL_FLY_BOOST = 0.15;
    private static LocalPlayer player;
    private static LocalPlayer boostedPlayer;
    private static float savedFlySpeed;
    private static boolean sprintToggled;
    private static boolean tapActive;
    private static boolean previousDown;
    private static boolean timingPress;
    private static long downAt;
    private static boolean ownsSprintKey;
    private static boolean suspended = true;
    private static InputConstants.Key sprintBinding;
    private static LocalPlayer sneakPlayer;
    private static boolean sneakEnabled;
    private static boolean sneakToggled;
    private static boolean sneakPreviousDown;
    private static boolean sneakTimingPress;
    private static long sneakDownAt;
    private static boolean ownsSneakKey;
    private static boolean sneakSuspended = true;
    private static InputConstants.Key sneakBinding;

    public static void tick(Minecraft c) {
        if (c.options == null) return;
        boolean tap = ModConfig.toggleSprint && option("tap_toggle");
        boolean vanillaToggle = ModConfig.toggleSprint && !tap;
        // ToggleKeyMapping.setDown toggles on true when vanilla toggle mode is enabled.
        // Disable that mode before writing our own pressed state.
        if (c.options.toggleSprint().get() != vanillaToggle) {
            if (vanillaToggle && ownsSprintKey) c.options.keySprint.setDown(false);
            c.options.toggleSprint().set(vanillaToggle);
        }
        sneakTick(c);

        KeyMapping key = c.options.keySprint;
        InputConstants.Key binding = KeyMappingHelper.getBoundKeyOf(key);
        boolean usable = c.player != null && c.screen == null && c.isWindowActive();
        boolean down = usable && physicallyDown(c, key);
        boolean contextChanged = player != c.player || tap != tapActive;
        boolean rebound = !binding.equals(sprintBinding);
        if (contextChanged) {
            restoreFly();
            sprintToggled = tap && option("start_on");
            player = c.player;
            tapActive = tap;
        }
        if (contextChanged || rebound || !usable) {
            timingPress = false;
            previousDown = down;
            if (ownsSprintKey && !vanillaToggle) key.setDown(down);
            ownsSprintKey = false;
        }
        sprintBinding = binding;

        if (!usable) {
            restoreFly();
            if (tap) while (key.consumeClick()) { }
            suspended = true;
            return;
        }
        boolean resumed = suspended || contextChanged || rebound;
        suspended = false;
        if (resumed) {
            // A release after closing a GUI or regaining focus is not a new tap.
            previousDown = down;
            timingPress = false;
        }

        if (tap) {
            int presses = 0;
            while (key.consumeClick()) presses++;
            long now = System.nanoTime();
            if (down && !previousDown) {
                downAt = now;
                timingPress = true;
            }
            boolean flying = c.player.getAbilities().flying;
            if (!down && previousDown && timingPress) {
                if (!flying && now - downAt <= SPRINT_TAP_NANOS) sprintToggled = !sprintToggled;
                timingPress = false;
            } else if (!down && !previousDown && !resumed && !flying) {
                // Preserve taps that begin and end between client ticks.
                if ((presses & 1) != 0) sprintToggled = !sprintToggled;
            }
            previousDown = down;
            key.setDown(down || (sprintToggled && !flying));
            ownsSprintKey = true;
        } else if (!ModConfig.toggleSprint && option("always_sprint")) {
            key.setDown(true);
            ownsSprintKey = true;
        } else if (ownsSprintKey) {
            if (!vanillaToggle) key.setDown(down);
            ownsSprintKey = false;
        }
        flightTick(c, down);
    }

    private static void sneakTick(Minecraft c) {
        // The custom latch owns toggle behavior; ToggleKeyMapping must remain in hold mode.
        if (c.options.toggleCrouch().get()) c.options.toggleCrouch().set(false);
        KeyMapping key = c.options.keyShift;
        InputConstants.Key binding = KeyMappingHelper.getBoundKeyOf(key);
        boolean gameplay = c.player != null && c.screen == null && c.isWindowActive();
        boolean down = gameplay && physicallyDown(c, key);
        boolean changed = sneakPlayer != c.player || sneakEnabled != ModConfig.toggleSneak;
        boolean rebound = !binding.equals(sneakBinding);
        if (changed) {
            sneakToggled = false;
            sneakPlayer = c.player;
            sneakEnabled = ModConfig.toggleSneak;
        }
        sneakBinding = binding;
        if (!sneakEnabled) {
            if (ownsSneakKey) key.setDown(down);
            ownsSneakKey = false;
            sneakTimingPress = false;
            sneakPreviousDown = down;
            sneakSuspended = true;
            return;
        }

        int presses = 0;
        while (key.consumeClick()) presses++;
        if (!gameplay) {
            sneakTimingPress = false;
            sneakPreviousDown = false;
            sneakSuspended = true;
            // Retain only an existing latch in inventory/container screens. Physical shift
            // events stay with the screen, so shift-clicking never toggles the latch.
            key.setDown(c.player != null && c.isWindowActive() && sneakToggled
                    && c.screen instanceof AbstractContainerScreen<?> && sneakInContainers(c));
            ownsSneakKey = true;
            return;
        }

        boolean resumed = sneakSuspended || changed || rebound;
        sneakSuspended = false;
        if (resumed) {
            sneakPreviousDown = down;
            sneakTimingPress = false;
        }
        long now = System.nanoTime();
        if (down && !sneakPreviousDown) {
            sneakDownAt = now;
            sneakTimingPress = true;
        }
        boolean flying = c.player.getAbilities().flying;
        if (!down && sneakPreviousDown && sneakTimingPress) {
            if (!flying && now - sneakDownAt <= SNEAK_TAP_NANOS) sneakToggled = !sneakToggled;
            sneakTimingPress = false;
        } else if (!down && !sneakPreviousDown && !resumed && !flying && (presses & 1) != 0) {
            sneakToggled = !sneakToggled;
        }
        sneakPreviousDown = down;
        key.setDown(down || sneakToggled);
        ownsSneakKey = true;
    }

    private static boolean sneakInContainers(Minecraft c) {
        if (!option("sneak_container")) return false;
        var network = c.getConnection();
        String brand = network == null ? null : network.serverBrand();
        // Match the original module's Hypixel server-brand exclusion.
        return brand == null || !brand.startsWith("Hypixel BungeeCord");
    }

    private static boolean option(String name) {
        return LegacyOptions.s("move_opts", name).b;
    }

    private static int flyAmount() {
        return Math.max(2, Math.min(8, (int) Math.round(LegacyOptions.s("move_opts", "fly_amount").n)));
    }

    private static boolean physicallyDown(Minecraft c, KeyMapping binding) {
        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(binding);
        int code = key.getValue();
        if (code < 0) return false;
        long window = c.getWindow().handle();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, code) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.SCANCODE) {
            for (int symbol = GLFW.GLFW_KEY_SPACE; symbol <= GLFW.GLFW_KEY_LAST; symbol++) {
                if (GLFW.glfwGetKeyScancode(symbol) == code
                        && GLFW.glfwGetKey(window, symbol) == GLFW.GLFW_PRESS) return true;
            }
        }
        return false;
    }

    private static void flightTick(Minecraft c, boolean sprintDown) {
        LocalPlayer p = c.player;
        var abilities = p.getAbilities();
        if (!ModConfig.toggleSprint || !option("fly_boost") || !sprintDown
                || !abilities.flying || !(abilities.instabuild || p.isSpectator())) {
            restoreFly();
            return;
        }
        if (boostedPlayer != p) {
            restoreFly();
            boostedPlayer = p;
            savedFlySpeed = abilities.getFlyingSpeed();
        }
        int amount = flyAmount();
        abilities.setFlyingSpeed(BASE_FLY_SPEED * amount);
        double vertical = 0;
        if (c.options.keyShift.isDown()) vertical -= VERTICAL_FLY_BOOST * amount;
        if (c.options.keyJump.isDown()) vertical += VERTICAL_FLY_BOOST * amount;
        if (vertical != 0) p.setDeltaMovement(p.getDeltaMovement().add(0, vertical, 0));
    }

    private static void restoreFly() {
        if (boostedPlayer != null) {
            boostedPlayer.getAbilities().setFlyingSpeed(savedFlySpeed);
            boostedPlayer = null;
        }
    }

    /** Reports the physical hold separately from the sprint toggle latch. */
    public static String statusText(Minecraft c) {
        var p = c.player;
        if (p == null || c.options == null) return "";
        boolean sprintHeld = c.isWindowActive() && c.screen == null
                && physicallyDown(c, c.options.keySprint);
        boolean sneakHeld = c.isWindowActive() && c.screen == null
                && physicallyDown(c, c.options.keyShift);
        String sneak = ModConfig.toggleSneak && sneakToggled && !sneakHeld ? "Toggled" : "Key Held";
        String sprint = sprintHeld ? "Key Held"
                : ModConfig.toggleSprint && (tapActive ? sprintToggled : c.options.keySprint.isDown())
                ? "Toggled" : "Vanilla";
        if (!option("detail")) {
            if (ModConfig.toggleSneak && p.isShiftKeyDown()) return "[Sneaking (" + sneak + ")]";
            if (ModConfig.toggleSprint && p.isSprinting()) return "[Sprinting (" + sprint + ")]";
            return "";
        }
        if (p.getAbilities().flying) {
            return boostedPlayer == p ? "[Flying (" + flyAmount() + "x Boost)]" : "[Flying]";
        }
        if (p.isPassenger()) return "[Riding]";
        if (p.isShiftKeyDown()) return "[Sneaking (" + sneak + ")]";
        if (p.isSwimming()) return "[Swimming]";
        if (p.isSprinting()) return "[Sprinting (" + sprint + ")]";
        return "";
    }
}
