package com.blairdash.blueclient.mods;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Option holders for the older features (Keystrokes, Armor HUD, Crosshair, Zoom, ...) that predate the
 * BlueMod framework. They have no card of their own (category "_OPTS"); the card of the feature opens
 * their settings screen, and ModManager saves them with the profile like any other mod.
 */
public final class LegacyOptions {
    private LegacyOptions() { }

    public static final String CAT = "_OPTS";

    /** Looks up one setting of a holder, e.g. s("zoom_opts", "fov"). */
    public static Setting s(String holder, String id) {
        BlueMod m = ModManager.get(holder);
        if (m == null) throw new IllegalStateException("no options holder " + holder);
        return m.setting(id);
    }

    public static BlueMod holder(String id) { return ModManager.get(id); }

    static void register(List<BlueMod> out) {
        out.add(new Crosshair());
        out.add(new LowFireOpts());
        out.add(new LowShieldOpts());
        out.add(new MotionBlurOpts());
        out.add(new KeystrokesOpts());
        out.add(new ArmorOpts());
        out.add(new HudStyleOpts());
        out.add(new ZoomOpts());
        out.add(new FreelookOpts());
        out.add(new MoveOpts());
        out.add(new TimeOpts());
        out.add(new WeatherOpts());
        out.add(new ChatOpts());
        out.add(new FullbrightOpts());
        out.add(new HitboxOpts());
        out.add(new HurtCamOpts());
        out.add(new ScoreboardOpts());
        out.add(new ParticleOpts());
    }

    /** Keeps one Setting and one legacy ModConfig field in step, whichever side changed. */
    private static final class Link {
        final Setting s; final DoubleSupplier get; final DoubleConsumer set; final double def;
        double last; boolean init;
        Link(Setting s, double def, DoubleSupplier get, DoubleConsumer set) { this.s = s; this.def = def; this.get = get; this.set = set; }
        double sv() { switch (s.kind) { case BOOL: return s.b ? 1 : 0; case COLOR: return s.color; default: return s.n; } }
        void put(double v) { switch (s.kind) { case BOOL: s.b = v != 0; break; case COLOR: s.color = (int) (long) v; break; default: s.n = v; } }
        void sync() {
            double cur = get.getAsDouble(), sv = sv();
            if (!init) {
                init = true;
                if (sv != def) { set.accept(sv); cur = sv; } else { put(cur); }
                last = cur;
                return;
            }
            if (cur != last) { put(cur); last = cur; }
            else if (sv != last) { set.accept(sv); last = sv; }
        }
    }

    private abstract static class Holder extends BlueMod {
        private final List<Link> links = new ArrayList<>();
        Holder(String id, String name) { super(id, name, CAT, null); }
        void link(String id, double def, DoubleSupplier get, DoubleConsumer set) { links.add(new Link(setting(id), def, get, set)); }
        void linkBool(String id, boolean def, java.util.function.BooleanSupplier get, java.util.function.Consumer<Boolean> set) {
            link(id, def ? 1 : 0, () -> get.getAsBoolean() ? 1 : 0, v -> set.accept(v != 0));
        }
        @Override public void tick(Minecraft c) { for (Link l : links) l.sync(); }
        @Override public void tickMenu(Minecraft c) { for (Link l : links) l.sync(); }
    }

    static final class LowFireOpts extends Holder {
        LowFireOpts() {
            super("lowfire_opts", "Low Fire");
            num("offset", "Fire offset (larger = lower)", 0, 0.8, 2, 0.45);
            link("offset", 0.45, () -> ModConfig.lowFireOffset, v -> ModConfig.lowFireOffset = (float) v);
        }
    }

    static final class LowShieldOpts extends Holder {
        LowShieldOpts() {
            super("lowshield_opts", "Low Shield");
            num("offset", "Shield offset (larger = lower)", 0, 0.8, 2, 0.25);
            link("offset", 0.25, () -> ModConfig.lowShieldOffset, v -> ModConfig.lowShieldOffset = (float) v);
        }
    }

    static final class MotionBlurOpts extends Holder {
        MotionBlurOpts() {
            super("motionblur_opts", "Motion Blur");
            num("strength", "Strength", 0, 1, 2, 0.5);
            link("strength", 0.5, () -> ModConfig.motionBlurStrength, v -> ModConfig.motionBlurStrength = (float) v);
        }
    }

    static final class Crosshair extends Holder {
        Crosshair() {
            super("crosshair_opts", "Crosshair");
            choice("preset", "Preset (pick to apply)", 0, CrosshairPresets.NAMES);
            bool("dot", "Center dot", false);
            num("dot_size", "Dot size", 1, 4, 0, 2);
            bool("outline", "Outline", false);
            color("outline_color", "Outline color", 0xFF000000);
            num("outline_thickness", "Outline thickness", 1, 3, 0, 1);
            bool("show_f5", "Show in third person (F5)", true);
            num("size", "Size", 2, 40, 0, 8);
            num("thickness", "Thickness", 1, 8, 0, 2);
            num("gap", "Gap", 0, 12, 0, 0);
            color("color", "Color", 0xFFFFFFFF);
            bool("enemy", "Enemy crosshair (color change on entity)", false);
            color("enemy_color", "Enemy color", 0xFFFF5555);
            bool("enemy_dot", "Enemy: center dot", false);
            num("enemy_dot_size", "Enemy: dot size", 1, 4, 0, 2);
            bool("enemy_outline", "Enemy: outline", false);
            color("enemy_outline_color", "Enemy: outline color", 0xFF000000);
            num("enemy_outline_thickness", "Enemy: outline thickness", 1, 3, 0, 1);
            num("enemy_size", "Enemy: size", 2, 40, 0, 8);
            num("enemy_thickness", "Enemy: thickness", 1, 8, 0, 2);
            num("enemy_gap", "Enemy: gap", 0, 12, 0, 0);
            choice("shape", "Shape", 0, CrosshairRenderer.SHAPES);
            num("scale", "Scale", 0.5, 4, 1, 1);
            choice("enemy_shape", "Enemy: shape", 0, CrosshairRenderer.SHAPES);
            num("enemy_scale", "Enemy: scale", 0.5, 4, 1, 1);
            bool("friendly", "Friendly crosshair (teammate in sight)", false);
            for (String fp : new String[] { "friendly_", "elytra_" }) {
                String fl = fp.equals("friendly_") ? "Friendly: " : "Elytra: ";
                int fc = fp.equals("friendly_") ? 0xFF55FF55 : 0xFFFFFFFF;
                if (fp.equals("elytra_")) bool("elytra", "Elytra crosshair (while gliding)", false);
                choice(fp + "shape", fl + "shape", 0, CrosshairRenderer.SHAPES);
                num(fp + "scale", fl + "scale", 0.5, 4, 1, 1);
                color(fp + "color", fl + "color", fc);
                num(fp + "size", fl + "size", 2, 40, 0, 8);
                num(fp + "thickness", fl + "thickness", 1, 8, 0, 2);
                num(fp + "gap", fl + "gap", 0, 12, 0, 0);
                bool(fp + "dot", fl + "center dot", false);
                num(fp + "dot_size", fl + "dot size", 1, 4, 0, 2);
                bool(fp + "outline", fl + "outline", false);
                color(fp + "outline_color", fl + "outline color", 0xFF000000);
                num(fp + "outline_thickness", fl + "outline thickness", 1, 3, 0, 1);
                bool(fp + "custom", fl + "custom pixel crosshair (EDIT PIXELS)", false);
            }
            num("dyn_bow", "Spread while drawing a bow (px)", 0, 12, 0, 0);
            num("dyn_attack", "Spread while swinging (px)", 0, 12, 0, 0);
            bool("show_spectator", "Show in spectator mode", false);
            choice("share_copy", "Share: copy my crosshair code", 0, "Click to copy", "Copied to clipboard!");
            choice("share_paste", "Share: paste a code from clipboard", 0, "Click to paste", "Imported!", "Invalid code");
            bool("grid_v2", "", false).hidden();            bool("custom", "Custom pixel crosshair (EDIT PIXELS)", false);
            num("custom_scale", "Custom pixel size (steps of 0.5)", 0.5, 8, 1, 1);
            num("editor_cell", "", 6, 40, 0, 10).hidden();
            bool("enemy_custom", "Enemy: custom pixel crosshair", false);
            for (int py = 0; py < 63; py++) {
                for (int pk = 0; pk < 8; pk++) {
                    num("cp" + py + "_" + pk, "", 0, 4294967295.0, 0, 0).hidden();
                    num("ecp" + py + "_" + pk, "", 0, 4294967295.0, 0, 0).hidden();
                    num("fcp" + py + "_" + pk, "", 0, 4294967295.0, 0, 0).hidden();
                    num("lcp" + py + "_" + pk, "", 0, 4294967295.0, 0, 0).hidden();
                }
            }
            num("grid_size", "", 5, 63, 0, 15).hidden();            link("size", 8, () -> ModConfig.crosshairSize, v -> ModConfig.crosshairSize = (float) v);
            link("thickness", 2, () -> ModConfig.crosshairThickness, v -> ModConfig.crosshairThickness = (float) v);
            link("gap", 0, () -> ModConfig.crosshairGap, v -> ModConfig.crosshairGap = (float) v);
            link("color", (double) 0xFFFFFFFF, () -> ModConfig.crosshairColor, v -> ModConfig.crosshairColor = (int) (long) v);
            linkBool("enemy", false, () -> ModConfig.enemyCrosshairEnabled, v -> ModConfig.enemyCrosshairEnabled = v);
            link("enemy_color", (double) 0xFFFF5555, () -> ModConfig.enemyCrosshairColor, v -> ModConfig.enemyCrosshairColor = (int) (long) v);
        }
        @Override public void tick(Minecraft c) { CrosshairPresets.tick(); super.tick(c); }
        @Override public void tickMenu(Minecraft c) { CrosshairPresets.tick(); super.tickMenu(c); }
    }

    static final class KeystrokesOpts extends Holder {
        KeystrokesOpts() {
            super("keystrokes_opts", "Keystrokes");
            num("scale", "Scale", 0.5, 2.0, 2, 1.0);
            bool("mouse", "Mouse buttons (LMB / RMB)", true);
            bool("space", "Spacebar", false);
            bool("cps", "Show CPS on mouse buttons", false);
            color("text_color", "Text color", 0xFFFFFFFF);
            color("pressed_text_color", "Pressed text color", 0xFF000000);
            color("bg_color", "Background color", 0xFF000000);
            num("bg_opacity", "Background opacity", 0.0, 1.0, 2, 0.5);
            color("pressed_color", "Pressed color", 0xFFFFFFFF);
            num("pressed_opacity", "Pressed opacity", 0.0, 1.0, 2, 0.8);
            bool("border", "Border", false);
            color("border_color", "Border color", 0xFFFFFFFF);
        }
    }

    static final class ArmorOpts extends Holder {
        ArmorOpts() {
            super("armor_opts", "Armor HUD");
            num("scale", "Scale", 0.5, 2.0, 2, 1.0);
            bool("hands", "Show main hand and offhand", true);
            bool("durability", "Show durability number", true);
            bool("percent", "Durability as percent", false);
            bool("dynamic_color", "Color by durability", true);
            color("text_color", "Text color (if not by durability)", 0xFFFFFFFF);
            num("low_threshold", "Low durability below (%)", 5, 50, 0, 25);
            bool("shadow", "Text shadow", true);
            bool("overlay", "Vanilla durability bar and count", true);
        }
    }

    /** Style shared by the older text HUDs: CPS/FPS, Coordinates, Potion HUD and the Info HUDs. */
    static final class HudStyleOpts extends Holder {
        HudStyleOpts() {
            super("hudstyle_opts", "HUD Style");
            color("text_color", "Text color", 0xFFFFFFFF);
            bool("shadow", "Text shadow", true);
            num("scale", "Scale", 0.5, 3.0, 2, 1.0);
            bool("brackets", "Brackets", false);
            color("bracket_color", "Bracket color", 0xFFAAAAAA);
            bool("background", "Background", false);
            color("bg_color", "Background color", 0xFF000000);
            num("bg_opacity", "Background opacity", 0.0, 1.0, 2, 0.4);
            bool("border", "Border", false);
            color("border_color", "Border color", 0xFFFFFFFF);
            num("border_thickness", "Border thickness", 1, 3, 0, 1);
        }
    }

    static final class ZoomOpts extends Holder {
        ZoomOpts() {
            super("zoom_opts", "Zoom");
            num("fov", "Zoom FOV (smaller = closer)", 4, 60, 0, 15);
            num("speed", "Zoom speed", 0.1, 1.0, 2, 0.5);
            bool("scroll", "Scroll wheel changes zoom", true);
            bool("toggle", "Toggle mode (press to zoom, press again to stop)", false);
        }
    }

    static final class FreelookOpts extends Holder {
        FreelookOpts() {
            super("freelook_opts", "Freelook");
            bool("invert_pitch", "Invert pitch", false);
            bool("invert_yaw", "Invert yaw", false);
            bool("toggle", "Toggle mode", false);
            num("sensitivity", "Sensitivity", 0.25, 2.0, 2, 1.0);
            choice("view", "Camera view", 0, "Third person back", "Third person front", "Stay first person");
        }
    }

    static final class MoveOpts extends Holder {
        MoveOpts() {
            super("move_opts", "Toggle Sprint / Sneak");
            bool("always_sprint", "Always sprint (hold forward)", false);
            bool("tap_toggle", "Tap sprint key to toggle (hold = normal sprint)", true);
            bool("start_on", "Start with sprint toggled on", true);
            bool("sneak_container", "Keep toggled sneak in containers (disabled on Hypixel)", false);
            bool("fly_boost", "Creative fly boost (hold sprint while flying)", true);
            num("fly_amount", "Fly boost amount", 2, 8, 0, 4);
        bool("detail", "Detailed status (Key Held / Vanilla / Flying / Riding)", true);
        }
    }

    static final class TimeOpts extends Holder {
        TimeOpts() {
            super("time_opts", "Time Changer");
            num("time", "Time of day (0 sunrise, 6000 noon, 12000 sunset, 18000 midnight)", 0, 24000, 0, 6000);
            bool("real_time", "Use my real clock", false);
            num("speed", "Time speed (0 = frozen)", 0, 20, 0, 0);
        }
    }

    static final class WeatherOpts extends Holder {
        WeatherOpts() {
            super("weather_opts", "Clear Weather");
            num("rain", "Rain strength (0 = none)", 0.0, 1.0, 2, 0.0);
            bool("thunder", "Allow thunder", false);
        }
    }

    static final class ChatOpts extends Holder {
        ChatOpts() {
            super("chat_opts", "Chat Timestamps");
            bool("twelve_hour", "12-hour clock", false);
            bool("seconds", "Show seconds", false);
            choice("brackets", "Brackets", 0, "[ ]", "( )", "None");
            color("color", "Timestamp color", 0xFFAAAAAA);
        }
    }

    static final class FullbrightOpts extends Holder {
        FullbrightOpts() {
            super("fullbright_opts", "Fullbright");
            num("brightness", "Brightness", 1.0, 16.0, 1, 16.0);
        }
    }
    static final class HitboxOpts extends Holder {
        HitboxOpts() {
            super("hitbox_opts", "Hitboxes");
            bool("players", "Players", true);
            bool("hostile", "Hostile mobs", true);
            bool("passive", "Passive mobs", true);
            bool("items", "Dropped items", true);
            bool("projectiles", "Projectiles", true);
            num("thickness", "Line thickness", 1, 5, 1, 1);
            color("players_color", "Player color", 0xFFFFFFFF);
            color("hostile_color", "Hostile mob color", 0xFFFFFFFF);
            color("passive_color", "Passive mob color", 0xFFFFFFFF);
            color("items_color", "Item color", 0xFFFFFFFF);
            color("projectiles_color", "Projectile color", 0xFFFFFFFF);
            bool("eye_line", "Show eye line", true);
            bool("look_vector", "Show look vector", true);
        }
    }

    static final class HurtCamOpts extends Holder {
        HurtCamOpts() {
            super("hurtcam_opts", "No Hurt Cam");
            num("intensity", "Intensity (0 off, 1 vanilla)", 0, 1, 2, 0);
        }
    }

    static final class ScoreboardOpts extends Holder {
        ScoreboardOpts() {
            super("scoreboard_opts", "Hide Scoreboard");
            bool("numbers_only", "Hide score numbers only", false);
        }
    }

    static final class ParticleOpts extends Holder {
        ParticleOpts() {
            super("particle_opts", "Particle Changer");
            // Keep the existing amount setting editable through the same card and profile key.
            BlueMod particles = ModManager.get("particle_changer");
            if (particles != null) settings.add(particles.setting("amount"));
            num("scale", "Particle size multiplier", 0.1, 5, 2, 1);
        }
    }
}