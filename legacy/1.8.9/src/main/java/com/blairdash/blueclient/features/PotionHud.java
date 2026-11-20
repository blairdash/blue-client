package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Lists active potion effects as "Effect Name II  45s" starting at the
 * configured X/Y. Effects with under 10 seconds left turn red.
 */
public class PotionHud {
    private static final int COLOR_NORMAL = 0xFFFFFFFF;
    private static final int COLOR_LOW = 0xFFFF5555;
    private static final int LOW_TICKS = 200; // 10 seconds

    @SubscribeEvent
    public void onRenderPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!ModConfig.potionHudEnabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;

        List<PotionEffect> effects = new ArrayList<PotionEffect>(mc.thePlayer.getActivePotionEffects());
        if (effects.isEmpty()) return;
        Collections.sort(effects, new Comparator<PotionEffect>() {
            public int compare(PotionEffect a, PotionEffect b) {
                return a.getEffectName().compareTo(b.getEffectName());
            }
        });

        int x = ModConfig.potionHudX;
        int y = ModConfig.potionHudY;
        for (PotionEffect effect : effects) {
            String name = StatCollector.translateToLocal(effect.getEffectName());
            if (effect.getAmplifier() > 0) {
                name += " " + roman(effect.getAmplifier() + 1);
            }

            String time;
            int color = COLOR_NORMAL;
            if (effect.getIsPotionDurationMax()) {
                time = "**";
            } else {
                time = ((effect.getDuration() + 19) / 20) + "s";
                if (effect.getDuration() < LOW_TICKS) color = COLOR_LOW;
            }

            mc.fontRendererObj.drawStringWithShadow(name + "  " + time, x, y, color);
            y += 10;
        }
    }

    private static String roman(int n) {
        String[] numerals = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" };
        return (n >= 1 && n < numerals.length) ? numerals[n] : String.valueOf(n);
    }
}
