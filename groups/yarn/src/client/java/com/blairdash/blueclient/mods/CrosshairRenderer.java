package com.blairdash.blueclient.mods;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the crosshair as filled rectangles {x1, y1, x2, y2, argb}. No Minecraft types here, so every
 * game version draws it the same way. Profiles: 0 Normal, 1 Friendly, 2 Enemy, 3 Elytra. Shapes: Cross, Circle,
 * Circle + dot, Square, Triangle, Arrow, or the pixel-grid crosshair.
 */
public final class CrosshairRenderer {
    private CrosshairRenderer() { }

    public static final String[] SHAPES = { "Cross", "Circle", "Circle + dot", "Square", "Triangle", "Arrow" };
    public static final String[] PROFILES = { "", "friendly_", "enemy_", "elytra_" };

    private static BlueMod holder;
    private static Setting st(String id) {
        if (holder == null) holder = ModManager.get("crosshair_opts");
        return holder.setting(id);
    }

    /** Which profile applies. {@code enemyTarget}: hostile mob or non-teammate player in the crosshair. */
    public static int pickProfile(boolean elytra, boolean teammateTarget, boolean enemyTarget, boolean enemyEnabled) {
        if (elytra && st("elytra").b) return 3;
        if (teammateTarget && st("friendly").b) return 1;
        if (enemyTarget && enemyEnabled) return 2;
        return 0;
    }

    /** Spread added to the gap from a drawn bow and from an attack swing (pixels). */
    public static double dynamic(boolean usingBow, int bowTicks, float swing) {
        double d = 0;
        if (usingBow) { double t = Math.min(bowTicks / 20.0, 1.0); d += st("dyn_bow").n * (1 - t * t); }
        if (swing > 0f && swing < 1f) { double k = 1 - swing; d += st("dyn_attack").n * k * k; }
        return d;
    }

    public static List<int[]> build(int cx, int cy, int profile, double dynamic) {
        String p = PROFILES[profile];
        List<int[]> out = new ArrayList<>();
        int color = st(p + "color").color;
        boolean custom = st(p + "custom").b;
        if (profile == 1 || profile == 3) custom = st(p + "custom").b;
        if (custom) {
            return PixelCrosshair.pixelRects(cx, cy, profile, dynamic / 8.0);
        }
        double scale = Math.max(0.25, st(p + "scale").n);
        int shape = Math.max(0, Math.min(SHAPES.length - 1, st(p + "shape").idx));
        int size = Math.max(2, (int) Math.round(st(p + "size").n * scale));
        int thick = Math.max(1, (int) Math.round(st(p + "thickness").n * scale));
        int gap = (int) Math.round(st(p + "gap").n * scale) + (int) Math.round(dynamic * 2);
        int dot = st(p + "dot").b ? Math.max(1, (int) Math.round(st(p + "dot_size").n * scale)) : 0;
        if (shape == 2) dot = Math.max(dot, Math.max(2, (int) Math.round(2 * scale)));
        int extra = (int) Math.round(dynamic * 2);

        List<int[]> shapeRects = new ArrayList<>();
        switch (shape) {
            case 1: case 2: ring(shapeRects, cx, cy, size / 2.0 + extra, thick); break;
            case 3: square(shapeRects, cx, cy, size / 2 + extra, thick); break;
            case 4: triangle(shapeRects, cx, cy, size + extra, size / 2 + extra, thick); break;
            case 5: arrow(shapeRects, cx, cy, size / 2 + extra, thick); break;
            default: cross(shapeRects, cx, cy, size / 2, gap, thick); break;
        }
        if (dot > 0) shapeRects.add(new int[] { cx - dot / 2, cy - dot / 2, cx - dot / 2 + dot, cy - dot / 2 + dot });

        if (st(p + "outline").b) {
            int o = Math.max(1, (int) st(p + "outline_thickness").n), oc = st(p + "outline_color").color;
            for (int[] r : shapeRects) out.add(new int[] { r[0] - o, r[1] - o, r[2] + o, r[3] + o, oc });
        }
        for (int[] r : shapeRects) out.add(new int[] { r[0], r[1], r[2], r[3], color });
        return out;
    }

    private static void cross(List<int[]> o, int cx, int cy, int len, int gap, int t) {
        int s = t % 2; // odd thickness is centered on a pixel, so the far arms start one pixel later to stay symmetric
        o.add(new int[] { cx - gap - len, cy - t / 2, cx - gap, cy - t / 2 + t });
        o.add(new int[] { cx + gap + s, cy - t / 2, cx + gap + s + len, cy - t / 2 + t });
        o.add(new int[] { cx - t / 2, cy - gap - len, cx - t / 2 + t, cy - gap });
        o.add(new int[] { cx - t / 2, cy + gap + s, cx - t / 2 + t, cy + gap + s + len });
    }

    private static void ring(List<int[]> o, int cx, int cy, double radius, int t) {
        double inner = Math.max(0, radius - t);
        int rows = (int) Math.ceil(radius);
        for (int dy = -rows; dy < rows; dy++) {
            double yy = dy + 0.5;
            if (Math.abs(yy) > radius) continue;
            int xo = (int) Math.round(Math.sqrt(radius * radius - yy * yy));
            int xi = Math.abs(yy) < inner ? (int) Math.round(Math.sqrt(inner * inner - yy * yy)) : 0;
            if (xo <= 0) continue;
            if (xi <= 0) { o.add(new int[] { cx - xo, cy + dy, cx + xo, cy + dy + 1 }); continue; }
            o.add(new int[] { cx - xo, cy + dy, cx - xi, cy + dy + 1 });
            o.add(new int[] { cx + xi, cy + dy, cx + xo, cy + dy + 1 });
        }
    }

    private static void square(List<int[]> o, int cx, int cy, int h, int t) {
        t = Math.min(t, h);
        o.add(new int[] { cx - h, cy - h, cx + h, cy - h + t });
        o.add(new int[] { cx - h, cy + h - t, cx + h, cy + h });
        o.add(new int[] { cx - h, cy - h + t, cx - h + t, cy + h - t });
        o.add(new int[] { cx + h - t, cy - h + t, cx + h, cy + h - t });
    }

    private static void triangle(List<int[]> o, int cx, int cy, int height, int halfBase, int t) {
        if (height < 2) return;
        int top = cy - height / 2;
        int run = t + (int) Math.round((double) halfBase / height);
        for (int y = 0; y < height - t; y++) {
            int xe = (int) Math.round(halfBase * (y + 0.5) / height);
            int w = Math.min(run, Math.max(1, xe));
            o.add(new int[] { cx - xe, top + y, cx - xe + w, top + y + 1 });
            o.add(new int[] { cx + xe - w, top + y, cx + xe, top + y + 1 });
        }
        o.add(new int[] { cx - halfBase, top + height - t, cx + halfBase, top + height });
    }

    private static void arrow(List<int[]> o, int cx, int cy, int len, int t) {
        int top = cy - len / 2;
        for (int y = 0; y < len; y++) {
            o.add(new int[] { cx - y - t, top + y, cx - y, top + y + 1 });
            o.add(new int[] { cx + y, top + y, cx + y + t, top + y + 1 });
        }
    }
}