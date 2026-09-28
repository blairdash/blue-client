package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lists active potion effects as "Effect Name II  45s" starting at the
 * configured X/Y. Effects with under 10 seconds left turn red.
 */
public class PotionHud {
    private static final int COLOR_NORMAL = 0xFFFFFFFF;
    private static final int COLOR_LOW = 0xFFFF5555;
    private static final int LOW_TICKS = 200; // 10 seconds

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (client.player == null || client.options.hudHidden) return;

        List<StatusEffectInstance> effects = new ArrayList<>(client.player.getStatusEffects());
        if (effects.isEmpty()) return;
        effects.sort(Comparator.comparing(StatusEffectInstance::getTranslationKey));

        int x = ModConfig.potionHudX;
        int y = ModConfig.potionHudY;
        for (StatusEffectInstance effect : effects) {
            String name = Text.translatable(effect.getTranslationKey()).getString();
            if (effect.getAmplifier() > 0) {
                name += " " + roman(effect.getAmplifier() + 1);
            }

            String time;
            int color = COLOR_NORMAL;
            if (effect.isInfinite()) {
                time = "**";
            } else {
                time = ((effect.getDuration() + 19) / 20) + "s";
                if (effect.getDuration() < LOW_TICKS) color = COLOR_LOW;
            }

            ctx.drawTextWithShadow(client.textRenderer, name + "  " + time, x, y, color);
            y += 10;
        }
    }

    private static String roman(int n) {
        String[] numerals = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };
        return (n >= 1 && n < numerals.length) ? numerals[n] : String.valueOf(n);
    }
}