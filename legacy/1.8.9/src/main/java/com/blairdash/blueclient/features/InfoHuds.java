package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Small text HUDs: 0 Ping, 1 Memory, 2 Clock, 3 Direction, 4 Combo, 5 Reach,
 * 6 Sprint/Sneak status. Default spot is the top-right, under Coordinates
 * (the status line defaults to just under the crosshair).
 */
public class InfoHuds {
    private static final String[] LABELS = { "Ping", "Memory", "Clock", "Direction", "Combo", "Reach", "Toggle Status" };
    private static final String[] SAMPLES = { "Ping: 000 ms", "Mem: 0000/0000 MB (00%)", "00:00:00", "Facing: NW (000°)",
            "Combo: 00", "Reach: 0.00", "[Sprinting (Toggled)]" };
    private static final String[] DIRS = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
    private static final SimpleDateFormat CLOCK = new SimpleDateFormat("HH:mm:ss");

    public static String label(int id) { return LABELS[id]; }
    public static String sample(int id) { return SAMPLES[id]; }

    public static String text(int id, Minecraft c) {
        switch (id) {
            case 0: {
                if (c.thePlayer == null || c.getNetHandler() == null) return "Ping: --";
                NetworkPlayerInfo e = c.getNetHandler().getPlayerInfo(c.thePlayer.getUniqueID());
                return e == null ? "Ping: --" : "Ping: " + e.getResponseTime() + " ms";
            }
            case 1: {
                Runtime r = Runtime.getRuntime();
                long used = (r.totalMemory() - r.freeMemory()) >> 20;
                long max = r.maxMemory() >> 20;
                return "Mem: " + used + "/" + max + " MB (" + (max == 0 ? 0 : used * 100 / max) + "%)";
            }
            case 2:
                return CLOCK.format(new Date());
            case 3: {
                if (c.thePlayer == null) return "Facing: -";
                float bearing = ((c.thePlayer.rotationYaw + 180f) % 360f + 360f) % 360f; // 0 = north
                return "Facing: " + DIRS[Math.round(bearing / 45f) % 8] + " (" + Math.round(bearing) + "°)";
            }
            case 4: return PvpStats.comboText();
            case 5: return PvpStats.reachText();
            default: return ToggleMovement.statusText(c);
        }
    }

    public static int effectiveX(int id, Minecraft c, int sw) {
        if (ModConfig.infoX[id] >= 0) return ModConfig.infoX[id];
        int w = c.fontRendererObj.getStringWidth(SAMPLES[id]);
        return id == 6 ? (sw - w) / 2 : sw - w - 6;
    }

    public static int effectiveY(int id, int sh) {
        if (ModConfig.infoY[id] >= 0) return ModConfig.infoY[id];
        return id == 6 ? sh / 2 + 16 : 18 + id * 12;
    }

    @SubscribeEvent
    public void onRenderPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft c = Minecraft.getMinecraft();
        if (c.thePlayer == null || c.gameSettings.hideGUI) return;
        int sw = event.resolution.getScaledWidth(), sh = event.resolution.getScaledHeight();
        for (int id = 0; id < ModConfig.INFO_COUNT; id++) {
            if (!ModConfig.infoEnabled[id]) continue;
            String t = text(id, c);
            if (t.isEmpty()) continue;
            int x = ModConfig.infoX[id] >= 0 ? ModConfig.infoX[id]
                    : (id == 6 ? (sw - c.fontRendererObj.getStringWidth(t)) / 2
                               : sw - c.fontRendererObj.getStringWidth(SAMPLES[id]) - 6);
            c.fontRendererObj.drawStringWithShadow(t, x, effectiveY(id, sh), 0xFFFFFFFF);
        }
    }
}
