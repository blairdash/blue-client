package com.blairdash.blueclient.mods;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla's crosshair attack indicator, pixel for pixel (taken from the game's own hud/crosshair_attack_indicator_* sprites),
 * as filled rectangles so any version can draw it with a plain fill call. No Minecraft types here.
 * Vanilla draws the bar at (guiWidth/2 - 8, guiHeight/2 + 9); progress fills (int)(progress * 17) columns.
 */
public final class VanillaIndicator {
    private VanillaIndicator() { }

    public static final int W = 16, BAR_H = 4, FULL_H = 16;

    private static final String[] BACKGROUND = {
        "0 0 0 FF3A3B3C 0 0 0 0 0 0 0 0 0 0 0 0",
        "FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C",
        "FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C FF3A3B3C 0 0",
        "0 0 0 FF3A3B3C 0 0 0 0 0 0 0 0 0 0 0 0",
    };
    private static final String[] PROGRESS = {
        "0 0 0 FF5B4A49 0 0 0 0 0 0 0 0 0 0 0 0",
        "FF826D6C FF6D5957 FF826D6C FF6D5957 FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA",
        "FF826D6C FF6D5957 FF826D6C FF6D5957 FFEAECF2 FFEAECF2 FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF 0 0",
        "0 0 0 FF5B4A49 0 0 0 0 0 0 0 0 0 0 0 0",
    };
    private static final String[] FULL = {
        "0 0 0 FF5B4A49 0 0 0 0 0 0 0 0 0 0 0 0",
        "FF826D6C FF6D5957 FF826D6C FF6D5957 FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFD7D9EA FFFFFFFF",
        "FF826D6C FF6D5957 FF826D6C FF6D5957 FFEAECF2 FFEAECF2 FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF FFFFFFFF 0 0",
        "0 0 0 FF5B4A49 0 0 0 0 0 0 0 0 0 0 0 0",
        "0 0 0 0 0 0 0 0 FFD7D9EA 0 0 0 0 0 0 0",
        "0 0 0 0 0 0 0 FFD7D9EA FFD7D9EA FFD7D9EA 0 0 0 0 0 0",
        "0 0 0 0 0 0 0 0 FFD7D9EA 0 0 0 0 0 0 0",
        "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0",
        "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0",
        "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0",
        "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0",
        "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0",
        "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0",
        "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0",
        "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0",
        "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0",
    };

    private static int[][] parse(String[] rows) {
        int[][] out = new int[rows.length][];
        for (int y = 0; y < rows.length; y++) {
            String[] p = rows[y].split(" ");
            out[y] = new int[p.length];
            for (int x = 0; x < p.length; x++) out[y][x] = p[x].equals("0") ? 0 : (int) Long.parseLong(p[x], 16);
        }
        return out;
    }

    private static final int[][] BG = parse(BACKGROUND), PR = parse(PROGRESS), FU = parse(FULL);

    /** Rectangles {x1, y1, x2, y2, argb} for one sprite at (x, y), keeping only the first {@code cols} columns. */
    private static void add(List<int[]> out, int[][] px, int x, int y, int cols) {
        for (int row = 0; row < px.length; row++) {
            int col = 0;
            while (col < Math.min(cols, px[row].length)) {
                int c = px[row][col];
                if (c == 0) { col++; continue; }
                int end = col + 1;
                while (end < Math.min(cols, px[row].length) && px[row][end] == c) end++;
                out.add(new int[] { x + col, y + row, x + end, y + row + 1, c });
                col = end;
            }
        }
    }

    /** The cooldown bar: background plus the filled part. progress is 0..1 (vanilla only draws it below 1). */
    public static List<int[]> bar(int x, int y, float progress) {
        List<int[]> out = new ArrayList<>();
        add(out, BG, x, y, W);
        add(out, PR, x, y, Math.max(0, Math.min(W, (int) (progress * 17.0f))));
        return out;
    }

    /** The "ready to hit" indicator shown when a living target is in reach at full charge. */
    public static List<int[]> full(int x, int y) {
        List<int[]> out = new ArrayList<>();
        add(out, FU, x, y, W);
        return out;
    }
}
