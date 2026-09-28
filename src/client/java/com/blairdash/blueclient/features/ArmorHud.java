package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Shows helmet, chestplate, leggings, boots, then (after a small gap)
 * mainhand and offhand, each with its item icon, durability bar, and
 * remaining durability. X/Y is the top-left of the item icons. If no
 * position is set (-1), it defaults to the bottom-right corner.
 */
public class ArmorHud {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };
    private static final int ROW_HEIGHT = 18;
    private static final int ICON_SIZE = 16;
    private static final int HANDS_GAP = 6;
    private static final int FIRST_HAND_INDEX = 4;
    private static final int MARGIN = 4;

    private static int contentHeight() {
        return (SLOTS.length - 1) * ROW_HEIGHT + HANDS_GAP + ICON_SIZE;
    }

    public static int defaultX(int scaledWidth) {
        return scaledWidth - ICON_SIZE - MARGIN - 2;
    }

    public static int defaultY(int scaledHeight) {
        return scaledHeight - MARGIN - contentHeight();
    }

    /** The X actually used: the configured value, or the bottom-right default. */
    public static int effectiveX(int scaledWidth) {
        return ModConfig.armorHudX >= 0 ? ModConfig.armorHudX : defaultX(scaledWidth);
    }

    public static int effectiveY(int scaledHeight) {
        return ModConfig.armorHudY >= 0 ? ModConfig.armorHudY : defaultY(scaledHeight);
    }

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (client.player == null || client.options.hudHidden) return;

        int iconX = effectiveX(ctx.getScaledWindowWidth());
        int top = effectiveY(ctx.getScaledWindowHeight());

        for (int i = 0; i < SLOTS.length; i++) {
            ItemStack stack = client.player.getEquippedStack(SLOTS[i]);
            if (stack.isEmpty()) continue;

            int rowY = top + i * ROW_HEIGHT + (i >= FIRST_HAND_INDEX ? HANDS_GAP : 0);
            ctx.drawItem(stack, iconX, rowY);
            ctx.drawStackOverlay(client.textRenderer, stack, iconX, rowY);

            if (stack.isDamageable()) {
                int max = stack.getMaxDamage();
                int remaining = max - stack.getDamage();
                float fraction = max > 0 ? (float) remaining / max : 1.0f;
                int color = fraction > 0.5f ? 0xFF55FF55 : (fraction > 0.25f ? 0xFFFFFF55 : 0xFFFF5555);

                String text = String.valueOf(remaining);
                int textWidth = client.textRenderer.getWidth(text);
                ctx.drawTextWithShadow(client.textRenderer, text, iconX - 4 - textWidth, rowY + 4, color);
            }
        }
    }
}