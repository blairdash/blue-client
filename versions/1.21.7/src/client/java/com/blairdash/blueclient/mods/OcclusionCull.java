package com.blairdash.blueclient.mods;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity occlusion culling: an entity is skipped when every sight line from the camera to it
 * runs through solid opaque blocks. Results are cached briefly so the cost stays tiny.
 * Errors always fall on the side of "visible".
 */
public final class OcclusionCull {
    private OcclusionCull() { }

    private static final Map<Integer, long[]> CACHE = new HashMap<>();
    private static final long TTL_MS = 120;

    public static boolean hidden(Entity e, double cx, double cy, double cz, double minDist) {
        double dx = e.getX() - cx, dy = e.getY() - cy, dz = e.getZ() - cz;
        if (dx * dx + dy * dy + dz * dz < minDist * minDist) return false;
        long now = System.nanoTime() / 1_000_000L;
        long[] ent = CACHE.get(e.getId());
        if (ent != null && now - ent[0] < TTL_MS) return ent[1] == 1;
        World w = e.getWorld();
        Box b = e.getBoundingBox();
        double mx = (b.minX + b.maxX) / 2, my = (b.minY + b.maxY) / 2, mz = (b.minZ + b.maxZ) / 2;
        boolean visible =
                !blocked(w, cx, cy, cz, mx, my, mz)
                || !blocked(w, cx, cy, cz, mx, b.maxY - 0.05, mz)
                || !blocked(w, cx, cy, cz, mx, b.minY + 0.05, mz)
                || !blocked(w, cx, cy, cz, b.minX, my, b.minZ)
                || !blocked(w, cx, cy, cz, b.maxX, my, b.maxZ)
                || !blocked(w, cx, cy, cz, b.minX, my, b.maxZ)
                || !blocked(w, cx, cy, cz, b.maxX, my, b.minZ);
        if (CACHE.size() > 4000) CACHE.clear();
        CACHE.put(e.getId(), new long[] { now, visible ? 0 : 1 });
        return !visible;
    }

    private static boolean blocked(World w, double sx, double sy, double sz, double tx, double ty, double tz) {
        double dx = tx - sx, dy = ty - sy, dz = tz - sz;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0) return false;
        int steps = (int) Math.ceil(len / 0.35);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            double d = t * len;
            if (d < 0.4 || d > len - 0.6) continue;
            pos.set(sx + dx * t, sy + dy * t, sz + dz * t);
            if (w.getBlockState(pos).isOpaqueFullCube()) return true;
        }
        return false;
    }
}
