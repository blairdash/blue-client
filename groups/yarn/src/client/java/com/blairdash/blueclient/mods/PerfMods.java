package com.blairdash.blueclient.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.item.Items;

import java.util.List;

/** Performance mods. */
final class PerfMods {
    private PerfMods() { }

    static void register(List<BlueMod> out) { out.add(new FpsBoost()); }

    static final class FpsBoost extends BlueMod {
        private final Setting cull = bool("cull", "Cull far entities", true);
        private final Setting cullMobs = num("cull_mobs", "Mob render distance", 16, 128, 0, 48);
        private final Setting cullItems = num("cull_items", "Item / frame / stand distance", 8, 64, 0, 32);
        private final Setting occlusion = bool("occlusion", "Hide entities behind walls", true);
        private final Setting occMin = num("occ_min", "Wall check starts at (blocks)", 4, 32, 0, 8);
        private final Setting particleDist = num("particle_dist", "Particle distance (0 = off)", 0, 64, 0, 32);
        private final Setting shadows = bool("shadows", "No entity shadows", true);
        private final Setting vsync = bool("vsync", "Turn VSync off", true);
        private final Setting unlimited = bool("unlimited", "Unlimited FPS", true);

        // originals, saved with the profile so toggling off (even after a restart) restores them
        private final Setting applied = bool("applied", "", false).hidden();
        private final Setting oShadows = bool("o_shadows", "", true).hidden();
        private final Setting oVsync = bool("o_vsync", "", true).hidden();
        private final Setting oFps = num("o_fps", "", 0, 300, 0, 0).hidden();

        FpsBoost() { super("fps_boost", "FPS Boost", "UTILITY", Items.REDSTONE_TORCH); enabled = true; }

        @Override public void tick(MinecraftClient c) { apply(c); }
        @Override public void tickMenu(MinecraftClient c) { apply(c); }

        private void apply(MinecraftClient c) {
            GameOptions o = c.options;
            if (o == null) return;
            if (enabled) {
                if (!applied.b) {
                    oShadows.b = o.getEntityShadows().getValue(); oVsync.b = o.getEnableVsync().getValue();
                    oFps.n = o.getMaxFps().getValue();
                    applied.b = true;
                }
                if (shadows.b) set(o.getEntityShadows(), false);
                if (vsync.b) set(o.getEnableVsync(), false);
                if (unlimited.b) set(o.getMaxFps(), 260);
            } else if (applied.b) {
                set(o.getEntityShadows(), oShadows.b); set(o.getEnableVsync(), oVsync.b);
                set(o.getMaxFps(), (int) oFps.n);
                applied.b = false;
            }
        }

        private static <T> void set(net.minecraft.client.option.SimpleOption<T> opt, T v) {
            if (!opt.getValue().equals(v)) opt.setValue(v);
        }
    }
}