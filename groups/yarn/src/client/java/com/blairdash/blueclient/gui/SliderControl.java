package com.blairdash.blueclient.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** A self-drawn horizontal slider: label and value on top, a draggable track below. */
final class SliderControl {
    final String label;
    final double min, max;
    final int decimals;
    final Supplier<Double> getter;
    final Consumer<Double> setter;

    int x, y, w;
    private boolean dragging;

    static final int HEIGHT = 26;

    SliderControl(String label, double min, double max, int decimals, Supplier<Double> getter, Consumer<Double> setter) {
        this.label = label;
        this.min = min;
        this.max = max;
        this.decimals = decimals;
        this.getter = getter;
        this.setter = setter;
    }

    void place(int x, int y, int w) {
        this.x = x;
        this.y = y;
        this.w = w;
    }

    private String valueText() {
        return String.format(Locale.ROOT, "%." + decimals + "f", getter.get());
    }

    void render(DrawContext c, TextRenderer tr, int mx, int my) {
        Ui.text(c, tr, label, x, y, Ui.TEXT);
        String v = valueText();
        Ui.text(c, tr, v, x + w - Ui.width(tr, v), y, Ui.MUTED);

        int ty = y + 14;
        Ui.rrect(c, x, ty, w, 6, Ui.FIELD);
        double frac = (getter.get() - min) / (max - min);
        frac = Math.max(0, Math.min(1, frac));
        int fillW = (int) (w * frac);
        if (fillW > 0) Ui.rrect(c, x, ty, Math.max(2, fillW), 6, Ui.ACCENT);
        int hx = x + fillW - 3;
        boolean hot = dragging || Ui.inside(mx, my, x - 4, ty - 4, w + 8, 14);
        Ui.rrect(c, Math.max(x - 1, Math.min(hx, x + w - 5)), ty - 2, 6, 10, hot ? 0xFFFFFFFF : 0xFFCFD8F0);
    }

    boolean mouseClicked(double mx, double my) {
        if (Ui.inside(mx, my, x - 4, y + 10, w + 8, 16)) {
            dragging = true;
            update(mx);
            return true;
        }
        return false;
    }

    boolean mouseDragged(double mx) {
        if (!dragging) return false;
        update(mx);
        return true;
    }

    /** Returns true if this slider was being dragged (so the caller can save). */
    boolean mouseReleased() {
        boolean was = dragging;
        dragging = false;
        return was;
    }

    private void update(double mx) {
        double frac = Math.max(0, Math.min(1, (mx - x) / (double) w));
        double v = min + (max - min) * frac;
        double scale = Math.pow(10, decimals);
        setter.accept(Math.round(v * scale) / scale);
    }
}
