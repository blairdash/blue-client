package com.blairdash.blueclient.mods;

/** One configurable value of a mod: a toggle, a slider, a list of choices, or a color swatch. */
public final class Setting {
    public enum Kind { BOOL, NUM, CHOICE, COLOR }

    /** Swatches offered by COLOR settings (ARGB). */
    public static final int[] PALETTE = {
            0xFFFFFFFF, 0xFF3D8BFF, 0xFF55FFFF, 0xFF55FF55, 0xFFFFFF55, 0xFFFFAA00,
            0xFFFF5555, 0xFFFF55FF, 0xFFAA55FF, 0xFFAAAAAA, 0xFF000000 };

    public final String id, label;
    public final Kind kind;
    public boolean hidden;          // saved but not shown in the options screen

    public boolean b;
    public double n;
    public int idx;
    public int color;

    public double min, max;
    public int decimals;
    public String[] options;

    private final boolean defB;
    private final double defN;
    private final int defIdx, defColor;

    private Setting(String id, String label, Kind kind, boolean b, double n, int idx, int color) {
        this.id = id; this.label = label; this.kind = kind;
        this.b = b; this.n = n; this.idx = idx; this.color = color;
        this.defB = b; this.defN = n; this.defIdx = idx; this.defColor = color;
    }

    public static Setting bool(String id, String label, boolean def) {
        return new Setting(id, label, Kind.BOOL, def, 0, 0, 0);
    }

    public static Setting num(String id, String label, double min, double max, int decimals, double def) {
        Setting s = new Setting(id, label, Kind.NUM, false, def, 0, 0);
        s.min = min; s.max = max; s.decimals = decimals;
        return s;
    }

    public static Setting choice(String id, String label, int def, String... options) {
        Setting s = new Setting(id, label, Kind.CHOICE, false, 0, def, 0);
        s.options = options;
        return s;
    }

    public static Setting color(String id, String label, int def) {
        return new Setting(id, label, Kind.COLOR, false, 0, 0, def);
    }

    public Setting hidden() { this.hidden = true; return this; }

    public String choiceText() { return options[Math.max(0, Math.min(idx, options.length - 1))]; }

    public void reset() { b = defB; n = defN; idx = defIdx; color = defColor; }

    /** Value for JSON. */
    public Object export() {
        switch (kind) {
            case BOOL: return b;
            case NUM: return n;
            case CHOICE: return idx;
            default: return color;
        }
    }

    /** Value from JSON (Gson hands numbers back as Double). */
    public void importValue(Object o) {
        if (o == null) return;
        try {
            switch (kind) {
                case BOOL: b = o instanceof Boolean ? (Boolean) o : Boolean.parseBoolean(o.toString()); break;
                case NUM: n = Math.max(min, Math.min(max, ((Number) o).doubleValue())); break;
                case CHOICE: idx = Math.max(0, Math.min(options.length - 1, ((Number) o).intValue())); break;
                default: color = ((Number) o).intValue(); break;
            }
        } catch (RuntimeException ignored) { }
    }
}
