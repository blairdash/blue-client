package com.blairdash.blueclient.mods;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom tooltip rows (AppleSkin-style food icons, shulker grid). A marker line is added to the item's
 * tooltip text and TooltipMarkerMixin swaps it for a real tooltip component when the tooltip is built.
 */
public final class TipLines {
    private TipLines() { }

    private static final char MARK = '';
    private static final Map<Integer, Object> PAY = new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<Integer, Object> e) { return size() > 256; }
    };
    private static int next;

    private record Food(int nutrition, float saturation) { }
    private record Box(List<ItemStack> items) { }

    private static String markText(Object payload) {
        synchronized (PAY) {
            int id = next++;
            PAY.put(id, payload);
            return MARK + Integer.toString(id);
        }
    }

    private static Object lookup(String s) {
        if (s.isEmpty() || s.charAt(0) != MARK) return null;
        try {
            synchronized (PAY) { return PAY.get(Integer.parseInt(s.substring(1))); }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- sizing --------------------------------------------------------------------------------
    private static int hungerBars(Food f) { int b = (int) Math.ceil(Math.abs(f.nutrition) / 2f); return b > 10 ? 1 : b; }
    private static String hungerText(Food f) { int b = (int) Math.ceil(Math.abs(f.nutrition) / 2f); return b > 10 ? "x" + (f.nutrition < 0 ? -b : b) : null; }
    private static int satBars(Food f) { int b = (int) Math.ceil(Math.abs(f.saturation) / 2f); return b > 10 || b == 0 ? 1 : b; }
    private static String satText(Food f) { int b = (int) Math.ceil(Math.abs(f.saturation) / 2f); return b > 10 || b == 0 ? "x" + (f.saturation < 0 ? -b : b) : null; }
    private static int boxRows(Box b) { return Math.max(1, Math.min(3, (b.items.size() + 8) / 9)); }

    private static int heightOf(Object p) {
        if (p instanceof Food) return 9 + 1 + 7 + 3;
        if (p instanceof Box b) return boxRows(b) * 18 + 2;
        return 0;
    }

    // ---- pixel art -----------------------------------------------------------------------------
    private static int[] half;
    private static int[] halfFood() {
        if (half == null) {
            int[] full = HudMods.Saturation.FOOD_FULL, empty = HudMods.Saturation.FOOD_EMPTY, h = new int[81];
            for (int i = 0; i < 81; i++) h[i] = (i % 9) < 5 ? full[i] : empty[i];
            half = h;
        }
        return half;
    }
    /** AppleSkin's 7x7 saturation icons: quarter, half, three-quarter, full, empty. */
    private static final int[][] SAT7 = {
            { 0x00000000, 0x00000000, 0xFF0A0A0A, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFF0A0A0A, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFF282828, 0xFF282828, 0xFF0A0A0A, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFF282828, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF0A0A0A, 0xFF282828, 0xFFC2A100, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF282828, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0x00000000 },
            { 0x00000000, 0x00000000, 0xFF0A0A0A, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFF0A0A0A, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFF282828, 0xFF282828, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFF282828, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF9F8609, 0xFF282828, 0xFFC2A100, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF282828, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0x00000000 },
            { 0x00000000, 0x00000000, 0xFF0A0A0A, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFFDDC241, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFF282828, 0xFF282828, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFF282828, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0xFF282828, 0xFFC2A100, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF282828, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0x00000000 },
            { 0x00000000, 0x00000000, 0xFFDDC241, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFFDDC241, 0xFF282828, 0xFFDDC241, 0x00000000, 0x00000000, 0x00000000, 0xFFC2A100, 0xFF282828, 0xFF282828, 0xFF282828, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF282828, 0xFF282828, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0xFF282828, 0xFFC2A100, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF282828, 0xFFC2A100, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF9F8609, 0xFF9F8609, 0x00000000 },
            { 0x00000000, 0x00000000, 0xFF0A0A0A, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFF0A0A0A, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFF282828, 0xFF282828, 0xFF0A0A0A, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFF282828, 0xFF0A0A0A, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF0A0A0A, 0xFF282828, 0xFF0A0A0A, 0xFF0A0A0A, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF282828, 0xFF0A0A0A, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0xFF0A0A0A, 0xFF0A0A0A, 0x00000000 }
};
    private static void blit7(DrawContext ctx, int x, int y, int[] px, float alpha) {
        for (int i = 0; i < 49; i++) {
            int p = px[i];
            int a = (int) ((p >>> 24) * alpha);
            if (a <= 3) continue;
            int xx = x + i % 7, yy = y + i / 7;
            ctx.fill(xx, yy, xx + 1, yy + 1, (a << 24) | (p & 0xFFFFFF));
        }
    }

    // ---- drawing -------------------------------------------------------------------------------
    private static void drawFood(TextRenderer tr, int x, int y, DrawContext ctx, Food f) {
        int hb = hungerBars(f), n = f.nutrition;
        int xx = x + (hb - 1) * 9;
        for (int i = 0; i < hb * 2; i += 2) {
            HudMods.Saturation.blit(ctx, xx, y, HudMods.Saturation.FOOD_EMPTY, 1f);
            int[] icon = n - 1 == i ? halfFood() : HudMods.Saturation.FOOD_FULL;
            HudMods.Saturation.blit(ctx, xx, y, icon, 0.25f);
            if (n > i) HudMods.Saturation.blit(ctx, xx, y, icon, 1f);
            xx -= 9;
        }
        String ht = hungerText(f);
        if (ht != null) ctx.drawText(tr, ht, x + 10, y + 1, 0xFFAAAAAA, true);

        int sb = satBars(f);
        float abs = Math.abs(f.saturation);
        int sy = y + 10;
        xx = x + (sb - 1) * 7;
        for (int i = 0; i < sb * 2; i += 2) {
            float eff = (abs - i) / 2f;
            int frame = eff >= 1 ? 3 : eff > 0.5 ? 2 : eff > 0.25 ? 1 : eff > 0 ? 0 : 4;
            blit7(ctx, xx, sy, SAT7[frame], abs <= i ? 0.5f : 1f);
            xx -= 7;
        }
        String st = satText(f);
        if (st != null) ctx.drawText(tr, st, x + 8, sy, 0xFFAAAAAA, true);
    }

    private static void drawBox(TextRenderer tr, int x, int y, DrawContext ctx, Box b) {
        int rows = boxRows(b);
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < 9; c++) {
                int sx = x + c * 18, sy = y + 1 + r * 18;
                ctx.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
                ctx.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
            }
        for (int i = 0; i < b.items.size() && i < 27; i++) {
            ItemStack s = b.items.get(i);
            int sx = x + (i % 9) * 18 + 1, sy = y + 2 + (i / 9) * 18;
            ctx.drawItem(s, sx, sy);
            ctx.drawStackOverlay(tr, s, sx, sy);
        }
    }

    public static Text food(int nutrition, float saturation) { return Text.literal(markText(new Food(nutrition, saturation))); }
    public static Text box(List<ItemStack> items) { return Text.literal(markText(new Box(new java.util.ArrayList<>(items)))); }

    /** Called by the mixin for every text line that becomes a tooltip component. */
    public static TooltipComponent resolve(OrderedText text) {
        StringBuilder sb = new StringBuilder();
        text.accept((i, style, cp) -> { sb.appendCodePoint(cp); return sb.length() < 12; });
        Object p = lookup(sb.toString());
        return p == null ? null : new Comp(p);
    }

    private static final class Comp implements TooltipComponent {
        private final Object p;
        Comp(Object p) { this.p = p; }
        public int getHeight() { return heightOf(p); }
        public int getHeight(TextRenderer tr) { return heightOf(p); }
        public int getWidth(TextRenderer tr) {
            if (p instanceof Food f) {
                String ht = hungerText(f), st = satText(f);
                int hl = hungerBars(f) * 9 + (ht == null ? 0 : tr.getWidth(ht) + 2);
                int sl = satBars(f) * 7 + (st == null ? 0 : tr.getWidth(st) + 2);
                return Math.max(hl, sl);
            }
            return p instanceof Box ? 162 : 0;
        }
        public void drawItems(TextRenderer tr, int x, int y, DrawContext ctx) { draw(tr, x, y, ctx); }
        public void drawItems(TextRenderer tr, int x, int y, int w, int h, DrawContext ctx) { draw(tr, x, y, ctx); }
        private void draw(TextRenderer tr, int x, int y, DrawContext ctx) {
            if (p instanceof Food f) drawFood(tr, x, y, ctx, f);
            else if (p instanceof Box b) drawBox(tr, x, y, ctx, b);
        }
    }
}
