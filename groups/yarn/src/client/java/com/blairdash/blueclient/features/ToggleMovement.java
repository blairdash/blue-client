package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.mods.LegacyOptions;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Adapts the 1.8.9 tap/hold sprint and sneak behavior to Fabric's movement bindings. */
public class ToggleMovement {
    private static final long SPRINT_TAP_NANOS = 400_000_000L;
    private static final long SNEAK_TAP_NANOS = 200_000_000L;
    private static final float BASE_FLY_SPEED = 0.05f;
    private static final double VERTICAL_FLY_BOOST = 0.15;
    private static ClientPlayerEntity player;
    private static ClientPlayerEntity boostedPlayer;
    private static float savedFlySpeed;
    private static boolean sprintToggled;
    private static boolean tapActive;
    private static boolean previousDown;
    private static boolean timingPress;
    private static long downAt;
    private static boolean ownsSprintKey;
    private static boolean suspended = true;
    private static InputUtil.Key sprintBinding;
    private static ClientPlayerEntity sneakPlayer;
    private static boolean sneakEnabled;
    private static boolean sneakToggled;
    private static boolean sneakPreviousDown;
    private static boolean sneakTimingPress;
    private static long sneakDownAt;
    private static boolean ownsSneakKey;
    private static boolean sneakSuspended = true;
    private static InputUtil.Key sneakBinding;

    public static void tick(MinecraftClient c) {
        if (c.options == null) return;
        boolean tap = ModConfig.toggleSprint && option("tap_toggle");
        boolean vanillaToggle = ModConfig.toggleSprint && !tap;
        // StickyKeyBinding.setPressed toggles on true when vanilla toggle mode is enabled.
        // Disable that mode before writing our own pressed state.
        if (c.options.getSprintToggled().getValue() != vanillaToggle) {
            if (vanillaToggle && ownsSprintKey) c.options.sprintKey.setPressed(false);
            c.options.getSprintToggled().setValue(vanillaToggle);
        }
        sneakTick(c);

        KeyBinding key = c.options.sprintKey;
        InputUtil.Key binding = KeyBindingHelper.getBoundKeyOf(key);
        boolean usable = c.player != null && c.currentScreen == null && c.isWindowFocused();
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
            if (ownsSprintKey && !vanillaToggle) key.setPressed(down);
            ownsSprintKey = false;
        }
        sprintBinding = binding;

        if (!usable) {
            restoreFly();
            if (tap) while (key.wasPressed()) { }
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
            while (key.wasPressed()) presses++;
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
            key.setPressed(down || (sprintToggled && !flying));
            ownsSprintKey = true;
        } else if (!ModConfig.toggleSprint && option("always_sprint")) {
            key.setPressed(true);
            ownsSprintKey = true;
        } else if (ownsSprintKey) {
            if (!vanillaToggle) key.setPressed(down);
            ownsSprintKey = false;
        }
        flightTick(c, down);
    }

    private static void sneakTick(MinecraftClient c) {
        // The custom latch owns toggle behavior; StickyKeyBinding must remain in hold mode.
        if (c.options.getSneakToggled().getValue()) c.options.getSneakToggled().setValue(false);
        KeyBinding key = c.options.sneakKey;
        InputUtil.Key binding = KeyBindingHelper.getBoundKeyOf(key);
        boolean gameplay = c.player != null && c.currentScreen == null && c.isWindowFocused();
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
            if (ownsSneakKey) key.setPressed(down);
            ownsSneakKey = false;
            sneakTimingPress = false;
            sneakPreviousDown = down;
            sneakSuspended = true;
            return;
        }

        int presses = 0;
        while (key.wasPressed()) presses++;
        if (!gameplay) {
            sneakTimingPress = false;
            sneakPreviousDown = false;
            sneakSuspended = true;
            // Retain only an existing latch in inventory/container screens. Physical shift
            // events stay with the screen, so shift-clicking never toggles the latch.
            key.setPressed(c.player != null && c.isWindowFocused() && sneakToggled
                    && c.currentScreen instanceof HandledScreen<?> && sneakInContainers(c));
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
        key.setPressed(down || sneakToggled);
        ownsSneakKey = true;
    }

    private static boolean sneakInContainers(MinecraftClient c) {
        if (!option("sneak_container")) return false;
        String brand = MovementServerBrand.get(c);
        // Match the original module's Hypixel server-brand exclusion.
        return brand == null || !brand.startsWith("Hypixel BungeeCord");
    }

    private static boolean option(String name) {
        return LegacyOptions.s("move_opts", name).b;
    }

    private static int flyAmount() {
        return Math.max(2, Math.min(8, (int) Math.round(LegacyOptions.s("move_opts", "fly_amount").n)));
    }

    private static boolean physicallyDown(MinecraftClient c, KeyBinding binding) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(binding);
        int code = key.getCode();
        if (code < 0) return false;
        long window = c.getWindow().getHandle();
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, code) == GLFW.GLFW_PRESS;
        }
        if (key.getCategory() == InputUtil.Type.KEYSYM) {
            return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
        }
        if (key.getCategory() == InputUtil.Type.SCANCODE) {
            for (int symbol = GLFW.GLFW_KEY_SPACE; symbol <= GLFW.GLFW_KEY_LAST; symbol++) {
                if (GLFW.glfwGetKeyScancode(symbol) == code
                        && GLFW.glfwGetKey(window, symbol) == GLFW.GLFW_PRESS) return true;
            }
        }
        return false;
    }

    private static void flightTick(MinecraftClient c, boolean sprintDown) {
        ClientPlayerEntity p = c.player;
        var abilities = p.getAbilities();
        if (!ModConfig.toggleSprint || !option("fly_boost") || !sprintDown
                || !abilities.flying || !(abilities.creativeMode || p.isSpectator())) {
            restoreFly();
            return;
        }
        if (boostedPlayer != p) {
            restoreFly();
            boostedPlayer = p;
            savedFlySpeed = abilities.getFlySpeed();
        }
        int amount = flyAmount();
        abilities.setFlySpeed(BASE_FLY_SPEED * amount);
        double vertical = 0;
        if (c.options.sneakKey.isPressed()) vertical -= VERTICAL_FLY_BOOST * amount;
        if (c.options.jumpKey.isPressed()) vertical += VERTICAL_FLY_BOOST * amount;
        if (vertical != 0) p.setVelocity(p.getVelocity().add(0, vertical, 0));
    }

    private static void restoreFly() {
        if (boostedPlayer != null) {
            boostedPlayer.getAbilities().setFlySpeed(savedFlySpeed);
            boostedPlayer = null;
        }
    }

    /** Reports the physical hold separately from the sprint toggle latch. */
    public static String statusText(MinecraftClient c) {
        var p = c.player;
        if (p == null || c.options == null) return "";
        boolean sprintHeld = c.isWindowFocused() && c.currentScreen == null
                && physicallyDown(c, c.options.sprintKey);
        boolean sneakHeld = c.isWindowFocused() && c.currentScreen == null
                && physicallyDown(c, c.options.sneakKey);
        String sneak = ModConfig.toggleSneak && sneakToggled && !sneakHeld ? "Toggled" : "Key Held";
        String sprint = sprintHeld ? "Key Held"
                : ModConfig.toggleSprint && (tapActive ? sprintToggled : c.options.sprintKey.isPressed())
                ? "Toggled" : "Vanilla";
        if (!option("detail")) {
            if (ModConfig.toggleSneak && p.isSneaking()) return "[Sneaking (" + sneak + ")]";
            if (ModConfig.toggleSprint && p.isSprinting()) return "[Sprinting (" + sprint + ")]";
            return "";
        }
        if (p.getAbilities().flying) {
            return boostedPlayer == p ? "[Flying (" + flyAmount() + "x Boost)]" : "[Flying]";
        }
        if (p.hasVehicle()) return "[Riding]";
        if (p.isSneaking()) return "[Sneaking (" + sneak + ")]";
        if (p.isSwimming()) return "[Swimming]";
        if (p.isSprinting()) return "[Sprinting (" + sprint + ")]";
        return "";
    }
}
