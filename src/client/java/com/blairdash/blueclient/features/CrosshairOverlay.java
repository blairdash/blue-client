package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

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

    private static boolean isLookingAtEntity(MinecraftClient client) {
        HitResult target = client.crosshairTarget;
        return target instanceof EntityHitResult;
    }

        private static void draw(DrawContext ctx, MinecraftClient client) {
        int cx = ctx.getScaledWindowWidth() / 2;
        int cy = ctx.getScaledWindowHeight() / 2;
        int len = (int) (ModConfig.crosshairSize / 2);
        int gap = (int) ModConfig.crosshairGap;
        int thickness = Math.max(1, (int) ModConfig.crosshairThickness);

        boolean onEnemy = ModConfig.enemyCrosshairEnabled && isLookingAtEntity(client);
        int color = onEnemy ? ModConfig.enemyCrosshairColor : ModConfig.crosshairColor;

        ctx.fill(cx - gap - len, cy - thickness / 2, cx - gap, cy - thickness / 2 + thickness, color);
        ctx.fill(cx + gap, cy - thickness / 2, cx + gap + len, cy - thickness / 2 + thickness, color);
        ctx.fill(cx - thickness / 2, cy - gap - len, cx - thickness / 2 + thickness, cy - gap, color);
        ctx.fill(cx - thickness / 2, cy + gap, cx - thickness / 2 + thickness, cy + gap + len, color);

        if (client.player != null) {
            float cooldown = client.player.getAttackCooldownProgress(0.0f);
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
    private static void drawAttackIndicator(DrawContext ctx, int cx, int cy, float progress) {
        int barWidth = 20;
        int barHeight = 2;
        int y = cy + 12;
        int x = cx - barWidth / 2;

        ctx.fill(x, y, x + barWidth, y + barHeight, 0x80000000); // dim background
        int filled = (int) (barWidth * progress);
        ctx.fill(x, y, x + filled, y + barHeight, 0xFFFFFF00); // yellow fill
    }
}