package com.blairdash.blueclient.features;

import com.blairdash.blueclient.BlueClientInit;
import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.mods.LegacyOptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;

/** Hold (or toggle) the freelook key to swing the camera around the player without turning them. */
public class Freelook {
    public static boolean active = false;
    public static float yaw, pitch;
    private static Perspective previous = Perspective.FIRST_PERSON;
    private static boolean toggled = false, prevPressed = false;

    private static boolean b(String id) { return LegacyOptions.s("freelook_opts", id).b; }
    private static double n(String id) { return LegacyOptions.s("freelook_opts", id).n; }

    public static void tick(MinecraftClient c) {
        boolean pressed = BlueClientInit.freelookKey != null && BlueClientInit.freelookKey.isPressed();
        boolean held;
        if (b("toggle")) {
            if (pressed && !prevPressed && c.currentScreen == null) toggled = !toggled;
            held = toggled;
        } else {
            toggled = false;
            held = pressed;
        }
        prevPressed = pressed;

        boolean want = ModConfig.freelookEnabled && c.player != null && c.currentScreen == null && held;
        if (want && !active) {
            active = true;
            yaw = c.player.getYaw();
            pitch = c.player.getPitch();
            previous = c.options.getPerspective();
            int view = LegacyOptions.s("freelook_opts", "view").idx;
            c.options.setPerspective(view == 1 ? Perspective.THIRD_PERSON_FRONT
                    : view == 2 ? Perspective.FIRST_PERSON : Perspective.THIRD_PERSON_BACK);
        } else if (!want && active) {
            active = false;
            c.options.setPerspective(previous);
        }
    }

    /** Called from the mouse-look mixin with the raw deltas. */
    public static void look(double dx, double dy) {
        float sens = (float) n("sensitivity");
        yaw += (float) dx * 0.15f * sens * (b("invert_yaw") ? -1f : 1f);
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f * sens * (b("invert_pitch") ? -1f : 1f)));
    }
}