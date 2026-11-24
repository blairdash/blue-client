package com.blairdash.blueclient.features;

import java.util.ArrayList;
import java.util.List;

/** Builds the crosshair as filled rectangles {x1, y1, x2, y2, argb}. Same geometry as the newer versions. */
public final class CrosshairRenderer {
    private CrosshairRenderer() { }

    public static double dynamic(boolean usingBow, int bowTicks, float swing) {
        double d = 0;
        if (usingBow) { double t = Math.min(bowTicks / 20.0, 1.0); d += CrosshairData.n("dyn_bow") * (1 - t * t); }
        if (swing > 0f && swing < 1f) { double k = 1 - swing; d += CrosshairData.n("dyn_attack") * k * k; }
        return d;
    }

    public static List<int[]> build(int cx, int cy, int profile, double dynamic) {
        String p = CrosshairData.PREFIX[profile];
        List<int[]> out = new ArrayList<int[]>();
        int color = CrosshairData.col(p + "color");
        if (CrosshairData.b(p + "custom")) return PixelCrosshair.pixelRects(cx, cy, profile, dynamic / 8.0);
        double scale = Math.max(0.25, CrosshairData.n(p + "scale"));
        int shape = Math.max(0, Math.min(CrosshairData.SHAPES.length - 1, CrosshairData.i(p + "shape")));
        int size = Math.max(2, (int) Math.round(CrosshairData.n(p + "size") * scale));
        int thick = Math.max(1, (int) Math.round(CrosshairData.n(p + "thickness") * scale));
        int gap = (int) Math.round(CrosshairData.n(p + "gap") * scale) + (int) Math.round(dynamic * 2);
        int dot = CrosshairData.b(p + "dot") ? Math.max(1, (int) Math.round(CrosshairData.n(p + "dot_size") * scale)) : 0;
        if (shape == 2) dot = Math.max(dot, Math.max(2, (int) Math.round(2 * scale)));
        int extra = (int) Math.round(dynamic * 2);

        List<int[]> r = new ArrayList<int[]>();
        switch (shape) {
            case 1: case 2: ring(r, cx, cy, size / 2.0 + extra, thick); break;
            case 3: square(r, cx, cy, size / 2 + extra, thick); break;
            case 4: triangle(r, cx, cy, size + extra, size / 2 + extra, thick); break;
            case 5: arrow(r, cx, cy, size / 2 + extra, thick); break;
            default: cross(r, cx, cy, size / 2, gap, thick); break;
        }
        if (dot > 0) r.add(new int[] { cx - dot / 2, cy - dot / 2, cx - dot / 2 + dot, cy - dot / 2 + dot });

        if (CrosshairData.b(p + "outline")) {
            int o = Math.max(1, (int) CrosshairData.n(p + "outline_thickness")), oc = CrosshairData.col(p + "outline_color");
            for (int[] q : r) out.add(new int[] { q[0] - o, q[1] - o, q[2] + o, q[3] + o, oc });
        }
        for (int[] q : r) out.add(new int[] { q[0], q[1], q[2], q[3], color });
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