package com.blairdash.blueclient.features;

/** Crosshair presets: picking one writes its values into the normal crosshair once; everything can still be tweaked. */
public final class CrosshairPresets {
    private CrosshairPresets() { }

    public static final String[] NAMES = { "Custom (no change)", "Vanilla", "Thin cross", "Classic cross", "Gap cross", "Dot", "Dot + cross", "Outlined cross", "Circle", "Circle + dot", "Square", "Triangle", "Arrow" };
    // size, thickness, gap, dot size (0 = none), outline (0/1), color argb
    private static final int[][] DATA = {
        null,
        { 8, 1, 0, 1, 0, 0xFFFFFFFF },
        { 6, 1, 2, 0, 0, 0xFFFFFFFF },
        { 10, 2, 0, 0, 0, 0xFFFFFFFF },
        { 8, 2, 3, 0, 0, 0xFFFFFFFF },
        { 2, 1, 0, 2, 0, 0xFFFFFFFF },
        { 8, 2, 3, 2, 0, 0xFFFFFFFF },
        { 10, 2, 2, 0, 1, 0xFFFFFFFF },
        { 14, 1, 0, 0, 0, 0xFFFFFFFF },
        { 14, 1, 0, 0, 0, 0xFFFFFFFF },
        { 12, 1, 0, 0, 0, 0xFFFFFFFF },
        { 14, 1, 0, 0, 0, 0xFFFFFFFF },
        { 12, 2, 0, 0, 0, 0xFFFFFFFF },
    };
    private static final int[] SHAPE_OF = { 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5 };

    public static void apply(int idx) {
        if (idx <= 0 || idx >= DATA.length) return;
        int[] d = DATA[idx];
        CrosshairData.set("size", d[0]);
        CrosshairData.set("thickness", d[1]);
        CrosshairData.set("gap", d[2]);
        CrosshairData.setBool("dot", d[3] > 0);
        if (d[3] > 0) CrosshairData.set("dot_size", d[3]);
        CrosshairData.setBool("outline", d[4] == 1);
        CrosshairData.setCol("color", d[5]);
        CrosshairData.setBool("custom", false);
        CrosshairData.set("shape", SHAPE_OF[idx]);
        CrosshairData.set("preset", idx);
    }
}