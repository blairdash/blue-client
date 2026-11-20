package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Lists active potion effects as "Effect Name II  45s" starting at the
 * configured X/Y. Effects with under 10 seconds left turn red.
 */
public class PotionHud {
    private static final int COLOR_NORMAL = 0xFFFFFFFF;
    private static final int COLOR_LOW = 0xFFFF5555;
    private static final int LOW_TICKS = 200; // 10 seconds

    public static void render(GuiGraphicsExtractor ctx, Minecraft client) {
        if (client.player == null || client.gui.hud.isHidden()) return;

        List<MobEffectInstance> effects = new ArrayList<>(client.player.getActiveEffects());
        if (effects.isEmpty()) return;
        effects.sort(Comparator.comparing(MobEffectInstance::getDescriptionId));

        int x = ModConfig.potionHudX;
        int y = ModConfig.potionHudY;
        List<String> out = new ArrayList<>();
        int[] cols = new int[effects.size()];
        int ci = 0;
        for (MobEffectInstance effect : effects) {
            String name = Component.translatable(effect.getDescriptionId()).getString();
            if (effect.getAmplifier() > 0) {
                name += " " + roman(effect.getAmplifier() + 1);
            }

            String time;
            int color = 0; // 0 = use the HUD Style text color
            if (effect.isInfiniteDuration()) {
                time = "**";
            } else {
                time = ((effect.getDuration() + 19) / 20) + "s";
                if (effect.getDuration() < LOW_TICKS) color = COLOR_LOW;
            }

            out.add(name + "  " + time);
            cols[ci++] = color;
        }
        HudStyle.block(ctx, client, out, cols, x, y);
    }

    private static String roman(int n) {
        String[] numerals = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };
        return (n >= 1 && n < numerals.length) ? numerals[n] : String.valueOf(n);
    }
}
