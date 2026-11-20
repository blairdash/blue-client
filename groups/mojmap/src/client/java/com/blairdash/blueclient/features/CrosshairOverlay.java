package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

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
                (ctx, tickDelta) -> {
            if (!ModConfig.customCrosshairEnabled) return;
            Minecraft client = Minecraft.getInstance();
            if (client.gui.hud.isHidden() || client.player == null) return;
            draw(ctx, client);
        });
    }

    private static boolean isLookingAtEntity(Minecraft client) {
        HitResult target = client.hitResult;
        return target instanceof EntityHitResult;
    }

        private static void draw(GuiGraphicsExtractor ctx, Minecraft client) {
        if (!com.blairdash.blueclient.mods.LegacyOptions.s("crosshair_opts", "show_f5").b
                && !client.options.getCameraType().isFirstPerson()) return;
        int cx = ctx.guiWidth() / 2;
        int cy = ctx.guiHeight() / 2;
        var opts = com.blairdash.blueclient.mods.LegacyOptions.holder("crosshair_opts");
        boolean onEnemy = ModConfig.enemyCrosshairEnabled && isLookingAtEntity(client);
        String p = onEnemy ? "enemy_" : "";
        int len = (int) ((onEnemy ? opts.setting("enemy_size").n : ModConfig.crosshairSize) / 2);
        int gap = (int) (onEnemy ? opts.setting("enemy_gap").n : ModConfig.crosshairGap);
        int thickness = Math.max(1, (int) (onEnemy ? opts.setting("enemy_thickness").n : ModConfig.crosshairThickness));
        int color = onEnemy ? ModConfig.enemyCrosshairColor : ModConfig.crosshairColor;

        int[][] arms = {
                { cx - gap - len, cy - thickness / 2, cx - gap, cy - thickness / 2 + thickness },
                { cx + gap, cy - thickness / 2, cx + gap + len, cy - thickness / 2 + thickness },
                { cx - thickness / 2, cy - gap - len, cx - thickness / 2 + thickness, cy - gap },
                { cx - thickness / 2, cy + gap, cx - thickness / 2 + thickness, cy + gap + len } };
        boolean outline = opts.setting(p + "outline").b;
        int o = Math.max(1, (int) opts.setting(p + "outline_thickness").n), oc = opts.setting(p + "outline_color").color;
        int dot = opts.setting(p + "dot").b ? Math.max(1, (int) opts.setting(p + "dot_size").n) : 0;
        if (onEnemy ? opts.setting("enemy_custom").b : opts.setting("custom").b) {
            outline = false; dot = 0; arms = new int[0][];
            for (int[] r : com.blairdash.blueclient.mods.PixelCrosshair.pixelRects(cx, cy, onEnemy)) ctx.fill(r[0], r[1], r[2], r[3], r[4]);
        }
        if (outline) {
            for (int[] a : arms) ctx.fill(a[0] - o, a[1] - o, a[2] + o, a[3] + o, oc);
            if (dot > 0) ctx.fill(cx - dot / 2 - o, cy - dot / 2 - o, cx - dot / 2 + dot + o, cy - dot / 2 + dot + o, oc);
        }
        for (int[] a : arms) ctx.fill(a[0], a[1], a[2], a[3], color);
        if (dot > 0) ctx.fill(cx - dot / 2, cy - dot / 2, cx - dot / 2 + dot, cy - dot / 2 + dot, color);

        if (client.player != null) {
            float cooldown = client.player.getAttackStrengthScale(0.0f);
            if (cooldown < 1.0f) {
                drawAttackIndicator(ctx, cx, cy, cooldown);
            }
        }
    }

    /**
     * A simple stand-in for vanilla's crosshair attack indicator, since the
     * vanilla one is drawn inside the same method InGameHudMixin cancels.
     * Not pixel-identical to vanilla's icon, but restores the cooldown
     * feedback: a small bar under the crosshair that fills as it resets.
     */
    private static void drawAttackIndicator(GuiGraphicsExtractor ctx, int cx, int cy, float progress) {
        int barWidth = 20;
        int barHeight = 2;
        int y = cy + 12;
        int x = cx - barWidth / 2;

        ctx.fill(x, y, x + barWidth, y + barHeight, 0x80000000); // dim background
        int filled = (int) (barWidth * progress);
        ctx.fill(x, y, x + filled, y + barHeight, 0xFFFFFF00); // yellow fill
    }
}