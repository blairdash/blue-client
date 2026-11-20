package com.blairdash.blueclient.features;

import com.blairdash.blueclient.mods.LegacyOptions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;

/** Shared classification for the hitbox renderer; uncategorized entities retain vanilla boxes. */
public final class HitboxOptions {
    private HitboxOptions() { }

    private static String category(Entity entity) {
        if (entity instanceof PlayerEntity) return "players";
        if (entity instanceof Monster) return "hostile";
        if (entity instanceof LivingEntity) return "passive";
        if (entity instanceof ItemEntity) return "items";
        if (entity instanceof ProjectileEntity) return "projectiles";
        return null;
    }

    public static boolean visible(Entity entity) {
        String category = category(entity);
        return category == null || LegacyOptions.s("hitbox_opts", category).b;
    }

    public static int color(Entity entity) {
        String category = category(entity);
        return category == null ? 0xFFFFFFFF : LegacyOptions.s("hitbox_opts", category + "_color").color;
    }

    public static float thickness() { return (float) LegacyOptions.s("hitbox_opts", "thickness").n; }
    public static boolean eyeLine() { return LegacyOptions.s("hitbox_opts", "eye_line").b; }
    public static boolean lookVector() { return LegacyOptions.s("hitbox_opts", "look_vector").b; }
}
