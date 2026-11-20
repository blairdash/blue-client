package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Small text HUDs: 0 Ping, 1 Memory, 2 Clock, 3 Direction, 4 Combo, 5 Reach,
 * 6 Sprint/Sneak status. Default spot is the top-right, under Coordinates
 * (the status line defaults to just under the crosshair).
 */
public class InfoHuds {
    private static final String[] LABELS = { "Ping", "Memory", "Clock", "Direction", "Combo", "Reach", "Toggle Status" };
    private static final String[] SAMPLES = { "Ping: 000 ms", "Mem: 0000/0000 MB (00%)", "00:00:00", "Facing: NW (000°)",
            "Combo: 00", "Reach: 0.00", "[Sprinting (Toggled)]" };
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String[] DIRS = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };

    public static String label(int id) { return LABELS[id]; }
    public static String sample(int id) { return SAMPLES[id]; }

    public static String text(int id, MinecraftClient c) {
        switch (id) {
            case 0: {
                if (c.player == null || c.getNetworkHandler() == null) return "Ping: --";
                PlayerListEntry e = c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());
                return e == null ? "Ping: --" : "Ping: " + e.getLatency() + " ms";
            }
            case 1: {
                Runtime r = Runtime.getRuntime();
                long used = (r.totalMemory() - r.freeMemory()) >> 20;
                long max = r.maxMemory() >> 20;
                return "Mem: " + used + "/" + max + " MB (" + (max == 0 ? 0 : used * 100 / max) + "%)";
            }
            case 2:
                return LocalTime.now().format(CLOCK);
            case 3: {
                if (c.player == null) return "Facing: -";
                float bearing = ((c.player.getYaw() + 180f) % 360f + 360f) % 360f; // 0 = north
                return "Facing: " + DIRS[Math.round(bearing / 45f) % 8] + " (" + Math.round(bearing) + "°)";
            }
            case 4: return PvpStats.comboText();
            case 5: return PvpStats.reachText();
            default: {
                if (c.player == null) return "";
                return ToggleMovement.statusText(c);
            }
        }
    }

    public static int effectiveX(int id, MinecraftClient c, int sw) {
        if (ModConfig.infoX[id] >= 0) return ModConfig.infoX[id];
        int w = HudStyle.width(c, SAMPLES[id]);
        return id == 6 ? (sw - w) / 2 : sw - w - 6;
    }

    public static int effectiveY(int id, int sh) {
        if (ModConfig.infoY[id] >= 0) return ModConfig.infoY[id];
        return id == 6 ? sh / 2 + 16 : 18 + id * 12;
    }

    public static void render(DrawContext ctx, MinecraftClient c) {
        if (c.player == null) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        for (int id = 0; id < ModConfig.INFO_COUNT; id++) {
            if (!ModConfig.infoEnabled[id]) continue;
            String t = text(id, c);
            if (t.isEmpty()) continue;
            int x = ModConfig.infoX[id] >= 0 ? ModConfig.infoX[id]
                    : (id == 6 ? (sw - HudStyle.width(c, t)) / 2 : sw - HudStyle.width(c, SAMPLES[id]) - 6);
            HudStyle.line(ctx, c, t, x, effectiveY(id, sh));
        }
    }
}
