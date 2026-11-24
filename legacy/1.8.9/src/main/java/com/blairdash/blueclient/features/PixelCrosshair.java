package com.blairdash.blueclient.features;

import java.util.ArrayList;
import java.util.List;

/**
 * Pixel-by-pixel custom crosshair. Three grids (0 Normal, 1 Friendly, 2 Enemy), each a fixed 63x63 canvas stored
 * as 8 pixels x 4 bits per number (same layout as the newer versions, so share codes match). The visible grid is
 * an odd n x n window centered on the canvas. 0 = empty, 1..11 = palette index + 1.
 */
public final class PixelCrosshair {
    private PixelCrosshair() { }

    public static final int MAX = CrosshairData.MAX_GRID, MIN = 5;
    public static final int[] SIZES = { 7, 15, 31, 63 };
    private static final String[] KEYS = { "cp", "fcp", "ecp" };

    public static int n() { return Math.max(MIN, Math.min(MAX, (int) CrosshairData.n("grid_size"))); }
    private static int off() { return (MAX - n()) / 2; }

    private static int rawGet(int e, int sx, int sy) {
        if (sx < 0 || sy < 0 || sx >= MAX || sy >= MAX) return 0;
        long v = (long) CrosshairData.n(KEYS[e] + sy + "_" + (sx / 8));
        return (int) ((v >> ((sx % 8) * 4)) & 15L);
    }

    public static int get(int e, int x, int y) { return rawGet(e, x + off(), y + off()); }

    public static void set(int e, int x, int y, int val) {
        if (x < 0 || y < 0 || x >= n() || y >= n()) return;
        int sx = x + off(), sy = y + off();
        String k = KEYS[e] + sy + "_" + (sx / 8);
        int o = (sx % 8) * 4;
        long v = (long) CrosshairData.n(k);
        v = (v & ~(15L << o)) | ((long) (val & 15) << o);
        CrosshairData.set(k, (double) v);
    }

    public static void clear(int e) {
        for (int y = 0; y < MAX; y++) for (int k = 0; k < 8; k++) CrosshairData.set(KEYS[e] + y + "_" + k, 0);
    }

    public static int argb(int v) { return CrosshairData.PALETTE[Math.max(1, Math.min(v, CrosshairData.PALETTE.length)) - 1]; }

    public static void resize(int delta) {
        int cur = n(), next = cur;
        if (delta > 0) { for (int i = SIZES.length - 1; i >= 0; i--) if (SIZES[i] > cur) next = SIZES[i]; }
        else { for (int i = 0; i < SIZES.length; i++) if (SIZES[i] < cur) next = SIZES[i]; }
        CrosshairData.set("grid_size", next);
    }

    /** The crosshair as filled rectangles, centered on (cx, cy). extraScale grows the grid for bow / swing spread. */
    public static List<int[]> pixelRects(int cx, int cy, int profile, double extraScale) {
        List<int[]> out = new ArrayList<int[]>();
        double s = Math.max(0.5, Math.round((CrosshairData.n("custom_scale") + extraScale) * 2) / 2.0);
        int n = n();
        double left = cx - n * s / 2, top = cy - n * s / 2;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int v = get(profile, x, y);
                if (v == 0) continue;
                int x1 = (int) Math.round(left + x * s), y1 = (int) Math.round(top + y * s);
                int x2 = Math.max(x1 + 1, (int) Math.round(left + (x + 1) * s)), y2 = Math.max(y1 + 1, (int) Math.round(top + (y + 1) * s));
                out.add(new int[] { x1, y1, x2, y2, argb(v) });
            }
        }
        return out;
    }
}