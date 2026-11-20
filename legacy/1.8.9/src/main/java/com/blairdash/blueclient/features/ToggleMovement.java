package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Tap/hold movement latches using MCP bindings and LWJGL2 physical input. */
public class ToggleMovement {
    private static final long SPRINT_TAP_NANOS = 400_000_000L;
    private static final long SNEAK_TAP_NANOS = 200_000_000L;
    private static final float BASE_FLY_SPEED = 0.05f;
    private static final double VERTICAL_FLY_BOOST = 0.15;
    private static EntityPlayerSP player;
    private static EntityPlayerSP boostedPlayer;
    private static float savedFlySpeed;
    private static boolean sprintToggled;
    private static boolean tapActive;
    private static boolean sprintEnabled;
    private static boolean previousDown;
    private static boolean timingPress;
    private static long downAt;
    private static boolean ownsSprintKey;
    private static boolean suspended = true;
    private static Integer sprintBinding;
    private static EntityPlayerSP sneakPlayer;
    private static boolean sneakEnabled;
    private static boolean sneakToggled;
    private static boolean sneakPreviousDown;
    private static boolean sneakTimingPress;
    private static long sneakDownAt;
    private static boolean ownsSneakKey;
    private static boolean sneakSuspended = true;
    private static Integer sneakBinding;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Minecraft c = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.START && event.side.isClient() && event.player == c.thePlayer) tick(c);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft c = Minecraft.getMinecraft();
        // PlayerTickEvent stops on disconnect; still release owned keys and restore flight.
        if (event.phase == TickEvent.Phase.END
                && (c.thePlayer == null || c.currentScreen != null || !c.inGameHasFocus || !focused(c))) tick(c);
    }

    public static boolean sprintToggled() { return sprintToggled; }
    public static boolean sneakToggled() { return sneakToggled; }

    public static void tick(Minecraft c) {
        if (c.gameSettings == null) return;
        boolean tap = ModConfig.toggleSprint && option("tap_toggle");
        // 1.8.9 has no vanilla toggle mode; emulate press-to-toggle when tap mode is off.
        boolean pressToggle = ModConfig.toggleSprint && !tap;
        sneakTick(c);

        KeyBinding key = c.gameSettings.keyBindSprint;
        Integer binding = key.getKeyCode();
        boolean usable = c.thePlayer != null && c.currentScreen == null && c.inGameHasFocus && focused(c);
        boolean down = usable && physicallyDown(c, key);
        boolean contextChanged = player != c.thePlayer || tap != tapActive || sprintEnabled != ModConfig.toggleSprint;
        boolean rebound = !binding.equals(sprintBinding);
        if (contextChanged) {
            restoreFly();
            sprintToggled = ModConfig.toggleSprint && option("start_on");
            player = c.thePlayer;
            tapActive = tap;
            sprintEnabled = ModConfig.toggleSprint;
        }
        if (contextChanged || rebound || !usable) {
            timingPress = false;
            previousDown = down;
            if (ownsSprintKey) setPressed(key, down);
            ownsSprintKey = false;
        }
        sprintBinding = binding;

        if (!usable) {
            restoreFly();
            if (ModConfig.toggleSprint) while (key.isPressed()) { }
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

        if (ModConfig.toggleSprint) {
            int presses = 0;
            while (key.isPressed()) presses++;
            long now = System.nanoTime();
            if (down && !previousDown) {
                downAt = now;
                timingPress = tap;
                if (pressToggle && !c.thePlayer.capabilities.isFlying) sprintToggled = !sprintToggled;
            }
            boolean flying = c.thePlayer.capabilities.isFlying;
            if (!down && previousDown && timingPress) {
                if (!flying && now - downAt <= SPRINT_TAP_NANOS) sprintToggled = !sprintToggled;
                timingPress = false;
            } else if (!down && !previousDown && !resumed && !flying) {
                // Preserve taps that begin and end between client ticks.
                if ((presses & 1) != 0) sprintToggled = !sprintToggled;
            }
            previousDown = down;
            setPressed(key, down || (sprintToggled && !flying));
            ownsSprintKey = true;
        } else if (!ModConfig.toggleSprint && option("always_sprint")) {
            setPressed(key, true);
            ownsSprintKey = true;
        } else if (ownsSprintKey) {
            setPressed(key, down);
            ownsSprintKey = false;
        }
        flightTick(c, down);
    }

    private static void sneakTick(Minecraft c) {
        KeyBinding key = c.gameSettings.keyBindSneak;
        Integer binding = key.getKeyCode();
        boolean gameplay = c.thePlayer != null && c.currentScreen == null && c.inGameHasFocus && focused(c);
        boolean down = gameplay && physicallyDown(c, key);
        boolean changed = sneakPlayer != c.thePlayer || sneakEnabled != ModConfig.toggleSneak;
        boolean rebound = !binding.equals(sneakBinding);
        if (changed) {
            sneakToggled = false;
            sneakPlayer = c.thePlayer;
            sneakEnabled = ModConfig.toggleSneak;
        }
        sneakBinding = binding;
        if (!sneakEnabled) {
            if (ownsSneakKey) setPressed(key, down);
            ownsSneakKey = false;
            sneakTimingPress = false;
            sneakPreviousDown = down;
            sneakSuspended = true;
            return;
        }

        int presses = 0;
        while (key.isPressed()) presses++;
        if (!gameplay) {
            sneakTimingPress = false;
            sneakPreviousDown = false;
            sneakSuspended = true;
            // Retain only an existing latch in inventory/container screens. Physical shift
            // events stay with the screen, so shift-clicking never toggles the latch.
            setPressed(key, c.thePlayer != null && focused(c) && sneakToggled
                    && c.currentScreen instanceof GuiContainer && sneakInContainers(c));
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
        boolean flying = c.thePlayer.capabilities.isFlying;
        if (!down && sneakPreviousDown && sneakTimingPress) {
            if (!flying && now - sneakDownAt <= SNEAK_TAP_NANOS) sneakToggled = !sneakToggled;
            sneakTimingPress = false;
        } else if (!down && !sneakPreviousDown && !resumed && !flying && (presses & 1) != 0) {
            sneakToggled = !sneakToggled;
        }
        sneakPreviousDown = down;
        setPressed(key, down || sneakToggled);
        ownsSneakKey = true;
    }

    private static boolean sneakInContainers(Minecraft c) {
        if (!option("sneak_container")) return false;
        String brand = c.thePlayer == null ? null : c.thePlayer.getClientBrand();
        // Match the original module's Hypixel server-brand exclusion.
        if (brand != null && brand.toLowerCase(java.util.Locale.ROOT).contains("hypixel")) return false;
        String address = c.getCurrentServerData() == null ? "" : c.getCurrentServerData().serverIP;
        return !address.toLowerCase(java.util.Locale.ROOT).contains("hypixel.net");
    }

    private static boolean option(String name) {
        if ("tap_toggle".equals(name)) return ModConfig.moveTapToggle;
        if ("start_on".equals(name)) return ModConfig.moveStartOn;
        if ("fly_boost".equals(name)) return ModConfig.moveFlyBoost;
        if ("sneak_container".equals(name)) return ModConfig.moveSneakContainer;
        if ("detail".equals(name)) return ModConfig.moveDetail;
        if ("always_sprint".equals(name)) return ModConfig.moveAlwaysSprint;
        return false;
    }

    private static int flyAmount() {
        return Math.max(2, Math.min(8, ModConfig.moveFlyAmount));
    }

    private static boolean focused(Minecraft c) {
        return Display.isActive();
    }

    private static void setPressed(KeyBinding key, boolean down) {
        KeyBinding.setKeyBindState(key.getKeyCode(), down);
    }

    private static boolean physicallyDown(Minecraft c, KeyBinding binding) {
        int code = binding.getKeyCode();
        if (code < 0) {
            int button = code + 100;
            return Mouse.isCreated() && button >= 0 && button < Mouse.getButtonCount() && Mouse.isButtonDown(button);
        }
        return Keyboard.isCreated() && code > 0 && code < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(code);
    }

    private static void flightTick(Minecraft c, boolean sprintDown) {
        EntityPlayerSP p = c.thePlayer;
        net.minecraft.entity.player.PlayerCapabilities abilities = p.capabilities;
        if (!ModConfig.toggleSprint || !option("fly_boost") || !sprintDown
                || !abilities.isFlying || !(abilities.isCreativeMode || p.isSpectator())) {
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
        if (c.gameSettings.keyBindSneak.isKeyDown()) vertical -= VERTICAL_FLY_BOOST * amount;
        if (c.gameSettings.keyBindJump.isKeyDown()) vertical += VERTICAL_FLY_BOOST * amount;
        p.motionY += vertical;
    }

    private static void restoreFly() {
        if (boostedPlayer != null) {
            boostedPlayer.capabilities.setFlySpeed(savedFlySpeed);
            boostedPlayer = null;
        }
    }

    /** Reports the physical hold separately from the sprint toggle latch. */
    public static String statusText(Minecraft c) {
        EntityPlayerSP p = c.thePlayer;
        if (p == null || c.gameSettings == null) return "";
        boolean sprintHeld = focused(c) && c.currentScreen == null
                && physicallyDown(c, c.gameSettings.keyBindSprint);
        boolean sneakHeld = focused(c) && c.currentScreen == null
                && physicallyDown(c, c.gameSettings.keyBindSneak);
        String sneak = ModConfig.toggleSneak && sneakToggled && !sneakHeld ? "Toggled" : "Key Held";
        String sprint = sprintHeld ? "Key Held"
                : ModConfig.toggleSprint && sprintToggled
                ? "Toggled" : "Vanilla";
        if (!option("detail")) {
            if (ModConfig.toggleSneak && p.isSneaking()) return "[Sneaking (" + sneak + ")]";
            if (ModConfig.toggleSprint && p.isSprinting()) return "[Sprinting (" + sprint + ")]";
            return "";
        }
        if (p.capabilities.isFlying) {
            return boostedPlayer == p ? "[Flying (" + flyAmount() + "x Boost)]" : "[Flying]";
        }
        if (p.isRiding()) return "[Riding]";
        if (p.isSneaking()) return "[Sneaking (" + sneak + ")]";
        if (p.isSprinting()) return "[Sprinting (" + sprint + ")]";
        return "";
    }
}
