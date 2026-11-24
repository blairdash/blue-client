package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Draws a simple cross-shaped crosshair whose size/color come from ModConfig.
 * The vanilla crosshair is hidden by InGameHudMixin so this doesn't just
 * draw on top of the default one. If enemyCrosshairEnabled is on and the
 * player is currently looking at an entity (mob or player), the crosshair
 * switches to enemyCrosshairColor instead of the normal color.
 */
public class CrosshairOverlay {
    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath("clientmod", "custom_crosshair"),
                ctx -> {
            if (!ModConfig.customCrosshairEnabled) return;
            Minecraft client = Minecraft.getInstance();
            if (client.gui.hud.isHidden() || client.player == null) return;
            draw(ctx, client);
        });
    }

    private static boolean teammate(Minecraft client, net.minecraft.world.entity.Entity e) {
        return e instanceof net.minecraft.world.entity.player.Player && client.player.isAlliedTo(e);
    }

    private static void draw(GuiGraphicsExtractor ctx, Minecraft client) {
        var opts = com.blairdash.blueclient.mods.LegacyOptions.holder("crosshair_opts");
        if (!opts.setting("show_f5").b && !client.options.getCameraType().isFirstPerson()) return;
        if (client.player.isSpectator() && !opts.setting("show_spectator").b) return;
        int cx = ctx.guiWidth() / 2;
        int cy = ctx.guiHeight() / 2;
        net.minecraft.world.entity.Entity target = client.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
        boolean team = target != null && teammate(client, target);
        boolean foe = target != null && !team && (target instanceof net.minecraft.world.entity.monster.Enemy || target instanceof net.minecraft.world.entity.player.Player);
        int profile = com.blairdash.blueclient.mods.CrosshairRenderer.pickProfile(client.player.isFallFlying(), team, foe, ModConfig.enemyCrosshairEnabled);
        boolean bow = client.player.isUsingItem() && client.player.getUseItem().is(net.minecraft.world.item.Items.BOW);
        double dyn = com.blairdash.blueclient.mods.CrosshairRenderer.dynamic(bow, client.player.getTicksUsingItem(), client.player.getSwingAnimation(0.0f));
        for (int[] r : com.blairdash.blueclient.mods.CrosshairRenderer.build(cx, cy, profile, dyn)) ctx.fill(r[0], r[1], r[2], r[3], r[4]);

        if (client.options.attackIndicator().get() == net.minecraft.client.AttackIndicatorStatus.CROSSHAIR) {
            drawVanillaIndicator(ctx, client, cx - 8, cy + 9);
        }
    }
    /**
     * Vanilla's crosshair attack indicator, pixel for pixel: the cooldown bar while it charges, and the
     * "ready" mark when a living target is in reach at full charge. Vanilla draws this inside the method
     * InGameHudMixin cancels, so it is redrawn here. (x, y) is the top-left of the bar; vanilla uses
     * (width / 2 - 8, height / 2 + 9). Returns whether anything was drawn.
     */
    public static boolean drawVanillaIndicator(GuiGraphicsExtractor ctx, Minecraft client, int x, int y) {
        if (client.player == null) return false;
        float f = client.player.getAttackStrengthScale(0.0f);
        boolean ready = false;
        if (f >= 1.0f && client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
            ready = client.player.getCurrentItemAttackStrengthDelay() > 5.0f && living.isAlive();
        }
        java.util.List<int[]> rects = ready ? com.blairdash.blueclient.mods.VanillaIndicator.full(x, y)
                : f < 1.0f ? com.blairdash.blueclient.mods.VanillaIndicator.bar(x, y, f) : null;
        if (rects == null) return false;
        for (int[] r : rects) ctx.fill(r[0], r[1], r[2], r[3], r[4]);
        return true;
    }
}