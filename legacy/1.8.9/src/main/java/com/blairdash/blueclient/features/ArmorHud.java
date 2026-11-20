package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Shows helmet, chestplate, leggings, boots, then (after a small gap) the
 * held mainhand item, each with its item icon and remaining durability.
 * X/Y is the top-left of the item icons. If no position is set (-1), it
 * defaults to the bottom-right corner.
 *
 * Unlike the 26.x branches there's no off-hand slot here: off-hand was
 * added in the 1.9 combat rewrite and doesn't exist in 1.8.9 at all
 * (InventoryPlayer has no second hand slot).
 */
public class ArmorHud {
    // InventoryPlayer.armorInventory index order: 0=boots, 1=leggings, 2=chestplate, 3=helmet.
    // Displayed helmet-first, so read it back-to-front.
    private static final int[] ARMOR_SLOTS = { 3, 2, 1, 0 };
    private static final int ROW_HEIGHT = 18;
    private static final int ICON_SIZE = 16;
    private static final int HANDS_GAP = 6;
    private static final int MARGIN = 4;
    private static final int ROW_COUNT = ARMOR_SLOTS.length + 1; // + mainhand

    public static int contentHeight() {
        return (ROW_COUNT - 1) * ROW_HEIGHT + HANDS_GAP + ICON_SIZE;
    }

    private static int defaultX(int scaledWidth) {
        return scaledWidth - ICON_SIZE - MARGIN - 2;
    }

    private static int defaultY(int scaledHeight) {
        return scaledHeight - MARGIN - contentHeight();
    }

    public static int effectiveX(int scaledWidth) {
        return ModConfig.armorHudX >= 0 ? ModConfig.armorHudX : defaultX(scaledWidth);
    }

    public static int effectiveY(int scaledHeight) {
        return ModConfig.armorHudY >= 0 ? ModConfig.armorHudY : defaultY(scaledHeight);
    }

    @SubscribeEvent
    public void onRenderPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!ModConfig.armorHudEnabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;

        int iconX = effectiveX(event.resolution.getScaledWidth());
        int top = effectiveY(event.resolution.getScaledHeight());

        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack stack = mc.thePlayer.inventory.armorInventory[ARMOR_SLOTS[i]];
            drawSlot(mc, stack, iconX, top + i * ROW_HEIGHT);
        }
        ItemStack heldStack = mc.thePlayer.inventory.getCurrentItem();
        drawSlot(mc, heldStack, iconX, top + ARMOR_SLOTS.length * ROW_HEIGHT + HANDS_GAP);
    }

    private static void drawSlot(Minecraft mc, ItemStack stack, int x, int y) {
        if (stack == null) return;

        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);

        if (stack.isItemStackDamageable()) {
            int max = stack.getMaxDamage();
            int remaining = max - stack.getItemDamage();
            float fraction = max > 0 ? (float) remaining / max : 1.0f;
            int color = fraction > 0.5f ? 0xFF55FF55 : (fraction > 0.25f ? 0xFFFFFF55 : 0xFFFF5555);

            String text = String.valueOf(remaining);
            int textWidth = mc.fontRendererObj.getStringWidth(text);
            mc.fontRendererObj.drawStringWithShadow(text, x - 4 - textWidth, y + 4, color);
        }
    }
}
