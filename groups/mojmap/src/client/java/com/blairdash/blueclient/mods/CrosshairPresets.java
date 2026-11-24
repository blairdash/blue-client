package com.blairdash.blueclient.mods;

/**
 * Crosshair presets. Picking one in the "Preset" option writes its values into the normal crosshair
 * settings once; after that every setting can still be tweaked by hand. "Custom" never changes anything.
 */
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
    private static int applied = -1;

    /** Called every tick (also in menus) by the Crosshair option holder. */
    public static void tick() {
        CrosshairShare.tick();
        Setting pick = LegacyOptions.s("crosshair_opts", "preset");
        if (applied < 0) { applied = pick.idx; return; }   // first look: keep whatever was saved
        if (pick.idx == applied) return;
        applied = pick.idx;
        if (pick.idx <= 0 || pick.idx >= DATA.length) return;
        int[] d = DATA[pick.idx];
        set("size", d[0]); set("thickness", d[1]); set("gap", d[2]);
        LegacyOptions.s("crosshair_opts", "dot").b = d[3] > 0;
        if (d[3] > 0) set("dot_size", d[3]);
        LegacyOptions.s("crosshair_opts", "outline").b = d[4] == 1;
        LegacyOptions.s("crosshair_opts", "color").color = d[5];
        LegacyOptions.s("crosshair_opts", "custom").b = false;
        int[] shapeOf = { 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5 };
        LegacyOptions.s("crosshair_opts", "shape").idx = pick.idx < shapeOf.length ? shapeOf[pick.idx] : 0;
    }

    private static void set(String id, double v) { LegacyOptions.s("crosshair_opts", id).n = v; }
}