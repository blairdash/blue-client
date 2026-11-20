package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
            if (!world.isClientSide() || !(entity instanceof LivingEntity target)) return InteractionResult.PASS;
            // A target still in its hurt animation can't take another hit, so don't count it.
            if (target.hurtTime > 0) return InteractionResult.PASS;
            long now = System.currentTimeMillis();
            combo++;
            lastHit = now;
            lastReach = reach(player, target);
            reachTime = now;
            return InteractionResult.PASS;
        });
    }

    private static double reach(Player p, LivingEntity target) {
        Vec3 eye = p.getEyePosition();
        AABB b = target.getBoundingBox();
        double x = Math.clamp(eye.x, b.minX, b.maxX);
        double y = Math.clamp(eye.y, b.minY, b.maxY);
        double z = Math.clamp(eye.z, b.minZ, b.maxZ);
        return eye.distanceTo(new Vec3(x, y, z));
    }

    public static void tick(Minecraft c) {
        if (c.player != null) {
            boolean hurt = c.player.hurtTime > 0;
            if (hurt && !prevHurt) combo = 0;
            prevHurt = hurt;
            if (combo > 0 && System.currentTimeMillis() - lastHit > COMBO_TIMEOUT_MS) combo = 0;
        }
        if (ModConfig.hitboxesEnabled != hitboxApplied && c.debugEntries != null) {
            c.debugEntries.setStatus(DebugScreenEntries.ENTITY_HITBOXES,
                    ModConfig.hitboxesEnabled ? DebugScreenEntryStatus.ALWAYS_ON : DebugScreenEntryStatus.NEVER);
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
