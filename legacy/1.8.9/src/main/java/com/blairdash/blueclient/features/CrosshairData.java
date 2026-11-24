package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ConfigStorage;
import com.blairdash.blueclient.ModConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every crosshair setting, with the same ids the 1.21.x/26.x builds use so share codes work across versions.
 * Normal-profile size/thickness/gap/color and the enemy toggle/color live in ModConfig (the old settings file);
 * everything else is kept in crosshair.json next to it. Kinds: 0 bool, 1 number, 2 choice, 3 color (ARGB).
 * Profiles: 0 Normal (no prefix), 1 Friendly ("friendly_"), 2 Enemy ("enemy_").
 */
public final class CrosshairData {
    private CrosshairData() { }

    public static final int BOOL = 0, NUM = 1, CHOICE = 2, COLOR = 3;
    public static final String[] SHAPES = { "Cross", "Circle", "Circle + dot", "Square", "Triangle", "Arrow" };
    public static final String[] PREFIX = { "", "friendly_", "enemy_" };
    public static final int[] PALETTE = { 0xFFFFFFFF, 0xFF3D8BFF, 0xFF55FFFF, 0xFF55FF55, 0xFFFFFF55, 0xFFFFAA00, 0xFFFF5555, 0xFFFF55FF, 0xFFAA55FF, 0xFFAAAAAA, 0xFF000000 };
    public static final int MAX_GRID = 63;

    static final class Def {
        final int kind; final double def, min, max;
        Def(int kind, double def, double min, double max) { this.kind = kind; this.def = def; this.min = min; this.max = max; }
    }

    private static final Map<String, Def> DEFS = new LinkedHashMap<String, Def>();
    private static final Map<String, Double> VAL = new HashMap<String, Double>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static void d(String id, int kind, double def, double min, double max) { DEFS.put(id, new Def(kind, def, min, max)); }

    static {
        d("preset", CHOICE, 0, 0, 12);
        d("show_f5", BOOL, 1, 0, 1);
        d("enemy", BOOL, 0, 0, 1);
        d("friendly", BOOL, 0, 0, 1);
        d("dyn_bow", NUM, 0, 0, 12);
        d("dyn_attack", NUM, 0, 0, 12);
        d("custom_scale", NUM, 1, 0.5, 8);
        d("grid_size", NUM, 15, 5, 63);
        d("editor_cell", NUM, 10, 6, 40);
        for (int p = 0; p < 3; p++) {
            String x = PREFIX[p];
            long col = p == 0 ? 0xFFFFFFFFL : p == 1 ? 0xFF55FF55L : 0xFFFF5555L;
            d(x + "shape", CHOICE, 0, 0, SHAPES.length - 1);
            d(x + "scale", NUM, 1, 0.5, 4);
            d(x + "color", COLOR, col, 0, 0);
            d(x + "size", NUM, 8, 2, 40);
            d(x + "thickness", NUM, 2, 1, 8);
            d(x + "gap", NUM, 0, 0, 12);
            d(x + "dot", BOOL, 0, 0, 1);
            d(x + "dot_size", NUM, 2, 1, 4);
            d(x + "outline", BOOL, 0, 0, 1);
            d(x + "outline_color", COLOR, 0xFF000000L, 0, 0);
            d(x + "outline_thickness", NUM, 1, 1, 3);
            d(x + "custom", BOOL, 0, 0, 1);
        }
        String[] rows = { "cp", "fcp", "ecp" };
        for (String r : rows) for (int y = 0; y < MAX_GRID; y++) for (int k = 0; k < 8; k++) d(r + y + "_" + k, NUM, 0, 0, 4294967295.0);
    }

    public static boolean isRow(String id) { return id.startsWith("cp") || id.startsWith("fcp") || id.startsWith("ecp"); }
    public static Iterable<String> ids() { return DEFS.keySet(); }
    public static int kind(String id) { Def f = DEFS.get(id); return f == null ? -1 : f.kind; }
    public static double min(String id) { return DEFS.get(id).min; }
    public static double max(String id) { return DEFS.get(id).max; }

    public static double n(String id) {
        if (id.equals("size")) return ModConfig.crosshairSize;
        if (id.equals("thickness")) return ModConfig.crosshairThickness;
        if (id.equals("gap")) return ModConfig.crosshairGap;
        if (id.equals("color")) return ModConfig.crosshairColor & 0xFFFFFFFFL;
        if (id.equals("enemy_color")) return ModConfig.enemyCrosshairColor & 0xFFFFFFFFL;
        if (id.equals("enemy")) return ModConfig.enemyCrosshairEnabled ? 1 : 0;
        Double v = VAL.get(id);
        if (v != null) return v;
        Def f = DEFS.get(id);
        return f == null ? 0 : f.def;
    }

    public static boolean b(String id) { return n(id) >= 0.5; }
    public static int i(String id) { return (int) Math.round(n(id)); }
    public static int col(String id) { return (int) (long) n(id); }

    public static void set(String id, double v) {
        Def f = DEFS.get(id);
        if (f != null && f.kind != COLOR && !isRow(id)) v = Math.max(f.min, Math.min(f.max, v));
        if (id.equals("size")) ModConfig.crosshairSize = (float) v;
        else if (id.equals("thickness")) ModConfig.crosshairThickness = (float) v;
        else if (id.equals("gap")) ModConfig.crosshairGap = (float) v;
        else if (id.equals("color")) ModConfig.crosshairColor = (int) (long) v;
        else if (id.equals("enemy_color")) ModConfig.enemyCrosshairColor = (int) (long) v;
        else if (id.equals("enemy")) ModConfig.enemyCrosshairEnabled = v >= 0.5;
        else VAL.put(id, v);
    }
    public static void setBool(String id, boolean v) { set(id, v ? 1 : 0); }
    public static void setCol(String id, int argb) { set(id, argb & 0xFFFFFFFFL); }

    private static Path file() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = localAppData != null ? Paths.get(localAppData) : Paths.get(System.getProperty("user.home"), ".config");
        return base.resolve("BlueClient").resolve("crosshair.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Map<String, Double> m = GSON.fromJson(new String(Files.readAllBytes(f), Charset.forName("UTF-8")), new TypeToken<Map<String, Double>>() { }.getType());
            if (m == null) return;
            for (Map.Entry<String, Double> e : m.entrySet()) if (DEFS.containsKey(e.getKey()) && e.getValue() != null) VAL.put(e.getKey(), e.getValue());
        } catch (Exception ignored) { }
    }

    public static void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Map<String, Double> m = new LinkedHashMap<String, Double>();
            for (Map.Entry<String, Double> e : VAL.entrySet()) {
                Def d = DEFS.get(e.getKey());
                if (d != null && Math.abs(e.getValue() - d.def) > 1e-9) m.put(e.getKey(), e.getValue());
            }
            Files.write(f, GSON.toJson(m).getBytes(Charset.forName("UTF-8")));
        } catch (Exception ignored) { }
        ConfigStorage.save();
    }
}