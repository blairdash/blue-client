package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Combo counter, reach display and the hitbox toggle. */
public class PvpStats {
    private static final long COMBO_TIMEOUT_MS = 3000;
    private static final long REACH_SHOW_MS = 4000;

    public static int combo = 0;
    private static long lastHit = 0;
    private static double lastReach = -1;
    private static long reachTime = 0;
    private static boolean prevHurt = false;
    private static boolean hitboxApplied = false;

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient() || !(entity instanceof LivingEntity target)) return ActionResult.PASS;
            // A target still in its hurt animation can't take another hit, so don't count it.
            if (target.hurtTime > 0) return ActionResult.PASS;
            long now = System.currentTimeMillis();
            combo++;
            lastHit = now;
            lastReach = reach(player, target);
            reachTime = now;
            return ActionResult.PASS;
        });
    }

    private static double reach(PlayerEntity p, LivingEntity target) {
        Vec3d eye = p.getEyePos();
        Box b = target.getBoundingBox();
        double x = MathHelper.clamp(eye.x, b.minX, b.maxX);
        double y = MathHelper.clamp(eye.y, b.minY, b.maxY);
        double z = MathHelper.clamp(eye.z, b.minZ, b.maxZ);
        return eye.distanceTo(new Vec3d(x, y, z));
    }

    public static void tick(MinecraftClient c) {
        if (c.player != null) {
            boolean hurt = c.player.hurtTime > 0;
            if (hurt && !prevHurt) combo = 0;
            prevHurt = hurt;
            if (combo > 0 && System.currentTimeMillis() - lastHit > COMBO_TIMEOUT_MS) combo = 0;
        }
        if (ModConfig.hitboxesEnabled != hitboxApplied && c.debugHudEntryList != null) {
            c.debugHudEntryList.setEntryVisibility(
                    net.minecraft.client.gui.hud.debug.DebugHudEntries.ENTITY_HITBOXES,
                    ModConfig.hitboxesEnabled
                            ? net.minecraft.client.gui.hud.debug.DebugHudEntryVisibility.ALWAYS_ON
                            : net.minecraft.client.gui.hud.debug.DebugHudEntryVisibility.NEVER);
            hitboxApplied = ModConfig.hitboxesEnabled;
        }
    }

    public static String comboText() {
        return "Combo: " + combo;
    }

    public static String reachText() {
        if (lastReach < 0 || System.currentTimeMillis() - reachTime > REACH_SHOW_MS) return "Reach: -";
        return String.format("Reach: %.2f", lastReach);
    }
}
