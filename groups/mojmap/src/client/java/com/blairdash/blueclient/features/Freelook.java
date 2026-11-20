package com.blairdash.blueclient.features;

import com.blairdash.blueclient.BlueClientInit;
import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.mods.LegacyOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/** Hold or toggle the freelook key to swing the camera without turning the player. */
public class Freelook {
    public static boolean active = false;
    public static float yaw, pitch;
    private static CameraType previous = CameraType.FIRST_PERSON;
    private static boolean toggled = false, prevPressed = false;

    private static boolean b(String id) { return LegacyOptions.s("freelook_opts", id).b; }
    private static double n(String id) { return LegacyOptions.s("freelook_opts", id).n; }

    public static void tick(Minecraft c) {
        boolean pressed = BlueClientInit.freelookKey != null && BlueClientInit.freelookKey.isDown();
        boolean held;
        if (b("toggle")) {
            if (pressed && !prevPressed && c.gui.screen() == null) toggled = !toggled;
            held = toggled;
        } else {
            toggled = false;
            held = pressed;
        }
        prevPressed = pressed;
        boolean want = ModConfig.freelookEnabled && c.player != null && c.gui.screen() == null && held;
        if (want && !active) {
            active = true;
            yaw = c.player.getYRot();
            pitch = c.player.getXRot();
            previous = c.options.getCameraType();
            int view = LegacyOptions.s("freelook_opts", "view").idx;
            c.options.setCameraType(view == 1 ? CameraType.THIRD_PERSON_FRONT
                    : view == 2 ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
        } else if (!want && active) {
            active = false;
            c.options.setCameraType(previous);
        }
    }

    /** Called from the mouse-look mixin with the raw deltas. */
    public static void look(double dx, double dy) {
        float sens = (float) n("sensitivity");
        yaw += (float) dx * 0.15f * sens * (b("invert_yaw") ? -1f : 1f);
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f * sens * (b("invert_pitch") ? -1f : 1f)));
    }
}
