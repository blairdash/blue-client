package com.blairdash.blueclient.mods;

import java.util.ArrayList;
import java.util.List;

/**
 * Pixel-by-pixel custom crosshair. Two grids (normal and enemy) live in hidden settings of the
 * "crosshair_opts" holder. Storage is a fixed MAX x MAX (63) canvas (8 pixels x 4 bits per setting); the
 * visible grid is an odd N x N window centered on it, so changing the grid size keeps the drawing centered.
 * 0 = empty, 1..11 = Setting.PALETTE index + 1. No Minecraft types here: callers draw the returned
 * rectangles {x1, y1, x2, y2, argb} themselves.
 */
public final class PixelCrosshair {
    private PixelCrosshair() { }

    public static final int MAX = 63, MIN = 5;
    /** Lunar-style grid sizes the +/- buttons step through. */
    public static final int[] SIZES = { 7, 15, 31, 63 };
    private static int selected = 1;
    /** Which grid the editor shows: 0 Normal, 1 Friendly, 2 Enemy, 3 Elytra. */
    private static int edit;
    private static final String[] KEYS = { "cp", "fcp", "ecp", "lcp" };

    public static boolean editingEnemy() { return edit == 2; }
    public static void open() { edit = 0; }

    private static BlueMod holder() { return ModManager.get("crosshair_opts"); }
    private static final Setting[][][] ROWS = new Setting[4][MAX][(MAX + 7) / 8];
    private static boolean migrated;

    private static Setting row(int e, int sy, int sx) {
        ensureMigrated();
        Setting s = ROWS[e][sy][sx / 8];
        if (s == null) s = ROWS[e][sy][sx / 8] = holder().setting(KEYS[e] + sy + "_" + (sx / 8));
        return s;
    }

    /** Older versions stored a 31x31 canvas; the canvas is now 63x63 with the same drawing centered in it. */
    private static void ensureMigrated() {
        if (migrated) return;
        migrated = true;
        Setting flag = holder().setting("grid_v2");
        if (flag.b) return;
        flag.b = true;
        for (int e = 0; e < 2; e++) {
            String k = e == 1 ? "ecp" : "cp";
            int[][] old = new int[31][31];
            boolean any = false;
            for (int y = 0; y < 31; y++) for (int x = 0; x < 31; x++) {
                long v = (long) holder().setting(k + y + "_" + (x / 8)).n;
                old[y][x] = (int) ((v >> ((x % 8) * 4)) & 15L);
                any |= old[y][x] != 0;
            }
            if (!any) continue;
            for (int y = 0; y < MAX; y++) for (int w = 0; w < (MAX + 7) / 8; w++) holder().setting(k + y + "_" + w).n = 0;
            for (int y = 0; y < 31; y++) for (int x = 0; x < 31; x++) {
                if (old[y][x] == 0) continue;
                Setting s = holder().setting(k + (y + 16) + "_" + ((x + 16) / 8));
                s.n = (double) (((long) s.n) | ((long) old[y][x] << (((x + 16) % 8) * 4)));
            }
        }
    }

    /** Visible grid size (any value from MIN to MAX). */
    public static int n() {
        int v = (int) holder().setting("grid_size").n;
        v = Math.max(MIN, Math.min(MAX, v));
        return v;
    }
    private static int off() { return (MAX - n()) / 2; }

    private static int rawGet(int e, int sx, int sy) {
        if (sx < 0 || sy < 0 || sx >= MAX || sy >= MAX) return 0;
        long v = (long) row(e, sy, sx).n;
        return (int) ((v >> ((sx % 8) * 4)) & 15L);
    }

    public static int get(int e, int x, int y) { return rawGet(e, x + off(), y + off()); }
    public static int get(int x, int y) { return get(edit, x, y); }

    public static void set(int x, int y, int val) {
        int sx = x + off(), sy = y + off();
        if (x < 0 || y < 0 || x >= n() || y >= n()) return;
        Setting s = row(edit, sy, sx);
        int o = (sx % 8) * 4;
        long v = (long) s.n;
        v = (v & ~(15L << o)) | ((long) (val & 15) << o);
        s.n = (double) v;
    }

    public static void clear() {
        for (int y = 0; y < MAX; y++) for (int sx = 0; sx < MAX; sx++) {
            Setting s = row(edit, y, sx);
            long v = (long) s.n;
            s.n = (double) (v & ~(15L << ((sx % 8) * 4)));
        }
    }

    private static int argb(int v) { return Setting.PALETTE[Math.max(1, Math.min(v, Setting.PALETTE.length)) - 1]; }

    /** The crosshair as filled rectangles, centered on (cx, cy). */
    public static List<int[]> pixelRects(int cx, int cy, boolean enemy) { return pixelRects(cx, cy, enemy ? 2 : 0, 0); }

    /** {@code extraScale} grows the grid (used for the bow / attack spread). */
    public static List<int[]> pixelRects(int cx, int cy, int profile, double extraScale) {
        List<int[]> out = new ArrayList<>();
        double s = Math.max(0.5, Math.round((holder().setting("custom_scale").n + extraScale) * 2) / 2.0);
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

    // --- editor ----------------------------------------------------------------------------------
    /** Editor cell size in GUI pixels: the saved zoom, limited so everything fits the screen. */
    public static int cell(int h) {
        int want = (int) holder().setting("editor_cell").n;
        int fit = Math.max(4, (h - 190) / n());
        return Math.max(4, Math.min(want, fit));
    }

    private static int gridSize(int h) { return n() * cell(h); }
    public static int gx(int w, int h) { return w / 2 - gridSize(h) / 2; }
    public static int gy(int h) { return Math.max(24, (h - (gridSize(h) + 130)) / 2 + 14); }
    public static int editBtnX(int w) { return w / 2 - 45; }
    private static int palX(int w) { return w / 2 - Setting.PALETTE.length * 14 / 2; }
    private static int palY(int h) { return gy(h) + gridSize(h) + 10; }
    public static int[] clearBtn(int w, int h) { return new int[] { w / 2 - 70, palY(h) + 22, 60, 18 }; }
    public static int[] backBtn(int w, int h) { return new int[] { w / 2 + 10, palY(h) + 22, 60, 18 }; }
    private static int[] tab(int w, int h, int i) { return new int[] { w / 2 - 95 + i * 48, palY(h) + 44, 46, 16 }; }
    public static int[] tabNormal(int w, int h) { return tab(w, h, 0); }
    public static int[] tabFriendly(int w, int h) { return tab(w, h, 1); }
    public static int[] tabEnemy(int w, int h) { return tab(w, h, 2); }
    public static int[] tabElytra(int w, int h) { return tab(w, h, 3); }
    public static int[] zoomMinus(int w, int h) { return new int[] { w / 2 - 70, palY(h) + 64, 60, 16 }; }
    public static int[] zoomPlus(int w, int h) { return new int[] { w / 2 + 10, palY(h) + 64, 60, 16 }; }
    public static int[] gridMinus(int w, int h) { return new int[] { w / 2 - 70, palY(h) + 84, 60, 16 }; }
    public static int[] gridPlus(int w, int h) { return new int[] { w / 2 + 10, palY(h) + 84, 60, 16 }; }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static List<int[]> editorRects(int w, int h, int mx, int my) {
        List<int[]> out = new ArrayList<>();
        out.add(new int[] { 0, 0, w, h, 0xFF0B1018 });
        int n = n(), c = cell(h), gx = gx(w, h), gy = gy(h), size = n * c;
        out.add(new int[] { gx - 3, gy - 3, gx + size + 2, gy + size + 2, 0xFF2A3450 });
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int v = get(x, y);
                int col = v > 0 ? argb(v) : ((x + y) % 2 == 0 ? 0xFF1B2433 : 0xFF161E2B);
                if (v == 0 && n % 2 == 1 && x == n / 2 && y == n / 2) col = 0xFF2F4B7A;
                out.add(new int[] { gx + x * c, gy + y * c, gx + (x + 1) * c - 1, gy + (y + 1) * c - 1, col });
            }
        }
        if (in(mx, my, gx, gy, size, size)) {
            int hx = gx + ((mx - gx) / c) * c, hy = gy + ((my - gy) / c) * c;
            out.add(new int[] { hx, hy, hx + c - 1, hy + 1, 0xFFFFFFFF });
            out.add(new int[] { hx, hy + c - 2, hx + c - 1, hy + c - 1, 0xFFFFFFFF });
        }
        for (int i = 0; i < Setting.PALETTE.length; i++) {
            int x = palX(w) + i * 14, y = palY(h);
            out.add(new int[] { x, y, x + 12, y + 12, selected == i + 1 ? 0xFFFFFFFF : 0xFF2A3450 });
            out.add(new int[] { x + 1, y + 1, x + 11, y + 11, Setting.PALETTE[i] });
        }
        int[][] btns = { clearBtn(w, h), backBtn(w, h), zoomMinus(w, h), zoomPlus(w, h), gridMinus(w, h), gridPlus(w, h) };
        for (int[] b : btns) {
            boolean hv = in(mx, my, b[0], b[1], b[2], b[3]);
            out.add(new int[] { b[0], b[1], b[0] + b[2], b[1] + b[3], hv ? 0xFF3D8BFF : 0xFF22304A });
        }
        int[][] tabs = { tabNormal(w, h), tabFriendly(w, h), tabEnemy(w, h), tabElytra(w, h) };
        for (int i = 0; i < 4; i++) {
            int[] b = tabs[i];
            boolean sel = i == edit;
            out.add(new int[] { b[0], b[1], b[0] + b[2], b[1] + b[3], sel ? 0xFF3D8BFF : (in(mx, my, b[0], b[1], b[2], b[3]) ? 0xFF2F4B7A : 0xFF22304A) });
        }
        return out;
    }

    private static void paint(int w, int h, double mx, double my, int button) {
        int c = cell(h), gx = gx(w, h), gy = gy(h), n = n();
        if (!in(mx, my, gx, gy, n * c, n * c)) return;
        int x = (int) ((mx - gx) / c), y = (int) ((my - gy) / c);
        if (button == 0) set(x, y, selected);
        else if (button == 1) set(x, y, 0);
    }

    private static void zoom(int h, int delta) {
        holder().setting("editor_cell").n = Math.max(4, Math.min(40, cell(h) + delta));
    }

    private static void resize(int delta) {
        int cur = n(), next = cur;
        if (delta > 0) { for (int i = SIZES.length - 1; i >= 0; i--) if (SIZES[i] > cur) next = SIZES[i]; }
        else { for (int i = 0; i < SIZES.length; i++) if (SIZES[i] < cur) next = SIZES[i]; }
        holder().setting("grid_size").n = Math.max(MIN, Math.min(MAX, next));
    }

    /** 0 = ignored, 1 = handled, 2 = BACK pressed. */
    public static int click(int w, int h, double mx, double my, int button) {
        if (button == 0) {
            int[] c = clearBtn(w, h), b = backBtn(w, h);
            int[] zm = zoomMinus(w, h), zp = zoomPlus(w, h), gm = gridMinus(w, h), gp = gridPlus(w, h);
            for (int i = 0; i < 4; i++) {
                int[] tb = tab(w, h, i);
                if (in(mx, my, tb[0], tb[1], tb[2], tb[3])) { edit = i; return 1; }
            }
            if (in(mx, my, zm[0], zm[1], zm[2], zm[3])) { zoom(h, -2); return 1; }
            if (in(mx, my, zp[0], zp[1], zp[2], zp[3])) { zoom(h, 2); return 1; }
            if (in(mx, my, gm[0], gm[1], gm[2], gm[3])) { resize(-1); return 1; }
            if (in(mx, my, gp[0], gp[1], gp[2], gp[3])) { resize(1); return 1; }
            if (in(mx, my, c[0], c[1], c[2], c[3])) { clear(); return 1; }
            if (in(mx, my, b[0], b[1], b[2], b[3])) return 2;
            for (int i = 0; i < Setting.PALETTE.length; i++) {
                if (in(mx, my, palX(w) + i * 14, palY(h), 12, 12)) { selected = i + 1; return 1; }
            }
        }
        paint(w, h, mx, my, button);
        return 1;
    }

    public static void drag(int w, int h, double mx, double my, int button) { paint(w, h, mx, my, button); }
}