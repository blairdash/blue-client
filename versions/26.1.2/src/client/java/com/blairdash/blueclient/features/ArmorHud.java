package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.mods.Draw;
import com.blairdash.blueclient.mods.LegacyOptions;
import com.blairdash.blueclient.mods.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Shows helmet, chestplate, leggings, boots, then (after a small gap) mainhand and offhand, each with
 * its item icon and remaining durability. X/Y is the top-left of the item icons; -1 = bottom-right.
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

    private static Setting s(String id) { return LegacyOptions.s("armor_opts", id); }
    private static float sc() { return (float) s("scale").n; }
    private static int slots() { return s("hands").b ? SLOTS.length : FIRST_HAND_INDEX; }

    public static int contentHeight() {
        int raw = (slots() - 1) * ROW_HEIGHT + (s("hands").b ? HANDS_GAP : 0) + ICON_SIZE;
        return Math.round(raw * sc());
    }

    public static int defaultX(int scaledWidth) {
        return scaledWidth - Math.round(ICON_SIZE * sc()) - MARGIN - 2;
    }

    public static int defaultY(int scaledHeight) {
        return scaledHeight - MARGIN - contentHeight();
    }

    public static int effectiveX(int scaledWidth) {
        return ModConfig.armorHudX >= 0 ? ModConfig.armorHudX : defaultX(scaledWidth);
    }

    public static int effectiveY(int scaledHeight) {
        return ModConfig.armorHudY >= 0 ? ModConfig.armorHudY : defaultY(scaledHeight);
    }

    public static void render(GuiGraphicsExtractor ctx, Minecraft client) {
        if (client.player == null || client.options.hideGui) return;

        float k = sc();
        int iconX = Math.round(effectiveX(ctx.guiWidth()) / k);
        int top = Math.round(effectiveY(ctx.guiHeight()) / k);
        Draw.push(ctx, k);

        for (int i = 0; i < slots(); i++) {
            ItemStack stack = client.player.getItemBySlot(SLOTS[i]);
            if (stack.isEmpty()) continue;

            int rowY = top + i * ROW_HEIGHT + (i >= FIRST_HAND_INDEX ? HANDS_GAP : 0);
            ctx.item(stack, iconX, rowY);
            if (s("overlay").b) ctx.itemDecorations(client.font, stack, iconX, rowY);

            if (s("durability").b && stack.isDamageableItem()) {
                int max = stack.getMaxDamage();
                int remaining = max - stack.getDamageValue();
                float fraction = max > 0 ? (float) remaining / max : 1.0f;
                float low = (float) s("low_threshold").n / 100f;
                int color = s("dynamic_color").b
                        ? (fraction > Math.min(1f, low * 2) ? 0xFF55FF55 : (fraction > low ? 0xFFFFFF55 : 0xFFFF5555))
                        : s("text_color").color;

                String text = s("percent").b ? Math.round(fraction * 100) + "%" : String.valueOf(remaining);
                int textWidth = Draw.width(client, text);
                Draw.text(ctx, client, text, iconX - 4 - textWidth, rowY + 4, color, s("shadow").b);
            }
        }
        Draw.pop(ctx);
    }
}