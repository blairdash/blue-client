package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Draws a simple cross-shaped crosshair whose size/color come from ModConfig.
 * The vanilla crosshair is hidden by InGameHudMixin so this doesn't just
 * draw on top of the default one. If enemyCrosshairEnabled is on and the
 * player is currently looking at an entity (mob or player), the crosshair
 * switches to enemyCrosshairColor instead of the normal color.
 */
public class CrosshairOverlay {
    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            if (!ModConfig.customCrosshairEnabled) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options.hudHidden || client.player == null) return;
            draw(ctx, client);
        });
    }

    private static boolean teammate(MinecraftClient client, net.minecraft.entity.Entity e) {
        return e instanceof net.minecraft.entity.player.PlayerEntity && client.player.isTeammate(e);
    }

    private static void draw(DrawContext ctx, MinecraftClient client) {
        var opts = com.blairdash.blueclient.mods.LegacyOptions.holder("crosshair_opts");
        if (!opts.setting("show_f5").b && !client.options.getPerspective().isFirstPerson()) return;
        if (client.player.isSpectator() && !opts.setting("show_spectator").b) return;
        int cx = ctx.getScaledWindowWidth() / 2;
        int cy = ctx.getScaledWindowHeight() / 2;
        net.minecraft.entity.Entity target = client.crosshairTarget instanceof EntityHitResult hit ? hit.getEntity() : null;
        boolean team = target != null && teammate(client, target);
        boolean foe = target != null && !team && (target instanceof net.minecraft.entity.mob.Monster || target instanceof net.minecraft.entity.player.PlayerEntity);
        int profile = com.blairdash.blueclient.mods.CrosshairRenderer.pickProfile(client.player.isGliding(), team, foe, ModConfig.enemyCrosshairEnabled);
        boolean bow = client.player.isUsingItem() && client.player.getActiveItem().isOf(net.minecraft.item.Items.BOW);
        double dyn = com.blairdash.blueclient.mods.CrosshairRenderer.dynamic(bow, client.player.getItemUseTime(), client.player.getHandSwingProgress(0.0f));
        for (int[] r : com.blairdash.blueclient.mods.CrosshairRenderer.build(cx, cy, profile, dyn)) ctx.fill(r[0], r[1], r[2], r[3], r[4]);

        if (client.options.getAttackIndicator().getValue() == net.minecraft.client.option.AttackIndicator.CROSSHAIR) {
            drawVanillaIndicator(ctx, client, cx - 8, cy + 9);
        }
    }
    /**
     * Vanilla's crosshair attack indicator, pixel for pixel: the cooldown bar while it charges, and the
     * "ready" mark when a living target is in reach at full charge. Vanilla draws this inside the method
     * InGameHudMixin cancels, so it is redrawn here. (x, y) is the top-left of the bar; vanilla uses
     * (width / 2 - 8, height / 2 + 9). Returns whether anything was drawn.
     */
    public static boolean drawVanillaIndicator(DrawContext ctx, MinecraftClient client, int x, int y) {
        if (client.player == null) return false;
        float f = client.player.getAttackCooldownProgress(0.0f);
        boolean ready = false;
        if (f >= 1.0f && client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof net.minecraft.entity.LivingEntity living) {
            ready = client.player.getAttackCooldownProgressPerTick() > 5.0f && living.isAlive();
        }
        java.util.List<int[]> rects = ready ? com.blairdash.blueclient.mods.VanillaIndicator.full(x, y)
                : f < 1.0f ? com.blairdash.blueclient.mods.VanillaIndicator.bar(x, y, f) : null;
        if (rects == null) return false;
        for (int[] r : rects) ctx.fill(r[0], r[1], r[2], r[3], r[4]);
        return true;
    }
}