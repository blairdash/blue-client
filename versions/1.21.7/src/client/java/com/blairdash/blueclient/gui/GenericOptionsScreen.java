package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Options panel for any {@link BlueMod}: toggles, sliders, choice buttons and color swatches. */
final class GenericOptionsScreen extends Screen {
    private final Screen parent;
    private final BlueMod mod;
    private final List<Setting> shown = new ArrayList<>();
    private final List<SliderControl> sliders = new ArrayList<>();
    private int scroll;
    private boolean editing;

    private static final int ROW = 22;
    private static final int SLIDER_ROW = SliderControl.HEIGHT + 6;

    GenericOptionsScreen(Screen parent, BlueMod mod) {
        super(Text.literal(mod.name + " options"));
        this.parent = parent;
        this.mod = mod;
        for (Setting s : mod.settings) {
            if (s.hidden) continue;
            shown.add(s);
            if (s.kind == Setting.Kind.NUM) {
                sliders.add(new SliderControl(s.label, s.min, s.max, s.decimals, () -> s.n, v -> s.n = v));
            } else sliders.add(null);
        }
    }

    private int rowHeight(int i) { return shown.get(i).kind == Setting.Kind.NUM ? SLIDER_ROW : ROW; }

    private int contentHeight() {
        int h = 0;
        for (int i = 0; i < shown.size(); i++) h += rowHeight(i);
        return h + (mod.isHud() ? 24 : 0);
    }

    private int pw() { return Math.min(width - 20, 320); }
    private int ph() { return Math.min(height - 20, 70 + Math.min(contentHeight(), 220) + 28); }
    private int px() { return (width - pw()) / 2; }
    private int py() { return (height - ph()) / 2; }
    private int bodyTop() { return py() + 32; }
    private int bodyBottom() { return py() + ph() - 34; }
    private int doneX() { return px() + pw() - 76; }
    private int doneY() { return py() + ph() - 26; }
    private int resetX() { return px() + 10; }
    private int maxScroll() { return Math.max(0, contentHeight() - (bodyBottom() - bodyTop())); }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        c.fill(0, 0, width, height, Ui.DIM);
        int px = px(), py = py(), pw = pw(), ph = ph();
        Ui.rrect(c, px - 1, py - 1, pw + 2, ph + 2, Ui.PANEL_EDGE);
        Ui.rrect(c, px, py, pw, ph, Ui.PANEL);
        c.fill(px, py, px + pw, py + 26, Ui.HEADER);
        Ui.text(c, textRenderer, mod.name.toUpperCase(), px + 10, py + 9, Ui.TEXT);

        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        c.enableScissor(px, bodyTop(), px + pw, bodyBottom());
        int y = bodyTop() - scroll + 4;
        for (int i = 0; i < shown.size(); i++) {
            Setting s = shown.get(i);
            int h = rowHeight(i);
            drawRow(c, s, sliders.get(i), px + 14, y, pw - 28, mx, my);
            y += h;
        }
        if (mod.isHud()) {
            Ui.text(c, textRenderer, "Drag it in EDIT HUD LAYOUT (left side of the menu).", px + 14, y + 4, Ui.MUTED);
        }
        c.disableScissor();

        boolean hr = Ui.inside(mx, my, resetX(), doneY(), 70, 18);
        Ui.rrect(c, resetX(), doneY(), 70, 18, hr ? Ui.CARD_HOVER : Ui.CARD);
        Ui.label(c, textRenderer, "RESET", resetX(), doneY(), 70, 18, Ui.MUTED);
        boolean hd = Ui.inside(mx, my, doneX(), doneY(), 66, 18);
        Ui.rrect(c, doneX(), doneY(), 66, 18, hd ? Ui.ACCENT : Ui.ACCENT_DARK);
        Ui.label(c, textRenderer, "DONE", doneX(), doneY(), 66, 18, 0xFFFFFFFF);

        if (mod.id.equals("crosshair_opts")) {
            int ex = com.blairdash.blueclient.mods.PixelCrosshair.editBtnX(width), ey = doneY();
            boolean he = Ui.inside(mx, my, ex, ey, 90, 18);
            Ui.rrect(c, ex, ey, 90, 18, he ? Ui.CARD_HOVER : Ui.CARD);
            Ui.label(c, textRenderer, "EDIT PIXELS", ex, ey, 90, 18, Ui.TEXT);
        }
        if (editing) {
            for (int[] r : com.blairdash.blueclient.mods.PixelCrosshair.editorRects(width, height, (int) mx, (int) my)) c.fill(r[0], r[1], r[2], r[3], r[4]);
            int[] cb = com.blairdash.blueclient.mods.PixelCrosshair.clearBtn(width, height), bb = com.blairdash.blueclient.mods.PixelCrosshair.backBtn(width, height);
            Ui.label(c, textRenderer, "CLEAR", cb[0], cb[1], cb[2], cb[3], Ui.TEXT);
            Ui.label(c, textRenderer, "BACK", bb[0], bb[1], bb[2], bb[3], Ui.TEXT);
            int[] tn = com.blairdash.blueclient.mods.PixelCrosshair.tabNormal(width, height), te = com.blairdash.blueclient.mods.PixelCrosshair.tabEnemy(width, height);
            Ui.label(c, textRenderer, "NORMAL", tn[0], tn[1], tn[2], tn[3], Ui.TEXT);
            Ui.label(c, textRenderer, "ENEMY", te[0], te[1], te[2], te[3], Ui.TEXT);
            int[] zm = com.blairdash.blueclient.mods.PixelCrosshair.zoomMinus(width, height), zp = com.blairdash.blueclient.mods.PixelCrosshair.zoomPlus(width, height);
            Ui.label(c, textRenderer, "ZOOM -", zm[0], zm[1], zm[2], zm[3], Ui.TEXT);
            Ui.label(c, textRenderer, "ZOOM +", zp[0], zp[1], zp[2], zp[3], Ui.TEXT);
            int[] gm = com.blairdash.blueclient.mods.PixelCrosshair.gridMinus(width, height), gp = com.blairdash.blueclient.mods.PixelCrosshair.gridPlus(width, height);
            Ui.label(c, textRenderer, "GRID -", gm[0], gm[1], gm[2], gm[3], Ui.TEXT);
            Ui.label(c, textRenderer, "GRID +", gp[0], gp[1], gp[2], gp[3], Ui.TEXT);
            Ui.text(c, textRenderer, "Left click: draw   Right click: erase", com.blairdash.blueclient.mods.PixelCrosshair.gx(width, height), com.blairdash.blueclient.mods.PixelCrosshair.gy(height) - 16, Ui.TEXT);
        }    }

    private void drawRow(DrawContext c, Setting s, SliderControl slider, int x, int y, int w, int mx, int my) {
        switch (s.kind) {
            case BOOL: {
                Ui.text(c, textRenderer, s.label, x, y + 5, Ui.TEXT);
                Ui.toggle(c, x + w - 26, y + 4, s.b, Ui.inside(mx, my, x, y, w, ROW));
                break;
            }
            case NUM: {
                slider.place(x, y, w);
                slider.render(c, textRenderer, mx, my);
                break;
            }
            case CHOICE: {
                Ui.text(c, textRenderer, s.label, x, y + 5, Ui.TEXT);
                String t = s.choiceText();
                int bw = Math.max(70, Ui.width(textRenderer, t) + 14);
                boolean hv = Ui.inside(mx, my, x + w - bw, y, bw, 17);
                Ui.card(c, x + w - bw, y, bw, 17, 8, hv ? Ui.SELECTED : Ui.FIELD, hv ? Ui.ACCENT : Ui.PANEL_EDGE);
                Ui.label(c, textRenderer, t, x + w - bw, y, bw, 17, Ui.TEXT);
                break;
            }
            default: {
                Ui.text(c, textRenderer, s.label, x, y + 5, Ui.TEXT);
                int n = Setting.PALETTE.length;
                int sx = x + w - n * 14;
                for (int i = 0; i < n; i++) {
                    int cx = sx + i * 14;
                    boolean sel = Setting.PALETTE[i] == s.color;
                    Ui.rrect(c, cx, y + 2, 12, 12, 3, sel ? 0xFFFFFFFF : Ui.PANEL_EDGE);
                    Ui.rrect(c, cx + 1, y + 3, 10, 10, 2, Setting.PALETTE[i]);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double clickX, double clickY, int clickButton) {
        if (pixelClick(clickX, clickY, clickButton)) return true;
        double mx = clickX, my = clickY;
        if (clickButton == 0) {
            if (Ui.inside(mx, my, doneX(), doneY(), 66, 18)) { close(); return true; }
            if (Ui.inside(mx, my, resetX(), doneY(), 70, 18)) {
                for (Setting s : mod.settings) if (!s.hidden) s.reset();
                CrosshairConfigStorage.save();
                return true;
            }
            if (my >= bodyTop() && my < bodyBottom()) {
                int px = px(), pw = pw();
                int y = bodyTop() - scroll + 4;
                for (int i = 0; i < shown.size(); i++) {
                    Setting s = shown.get(i);
                    int x = px + 14, w = pw - 28;
                    switch (s.kind) {
                        case BOOL:
                            if (Ui.inside(mx, my, x, y, w, ROW)) { s.b = !s.b; CrosshairConfigStorage.save(); return true; }
                            break;
                        case NUM:
                            if (sliders.get(i).mouseClicked(mx, my)) return true;
                            break;
                        case CHOICE: {
                            String t = s.choiceText();
                            int bw = Math.max(70, Ui.width(textRenderer, t) + 14);
                            if (Ui.inside(mx, my, x + w - bw, y, bw, 17)) {
                                s.idx = (s.idx + 1) % s.options.length;
                                CrosshairConfigStorage.save();
                                return true;
                            }
                            break;
                        }
                        default: {
                            int n = Setting.PALETTE.length, sx = x + w - n * 14;
                            for (int k = 0; k < n; k++) {
                                if (Ui.inside(mx, my, sx + k * 14, y + 2, 12, 12)) {
                                    s.color = Setting.PALETTE[k];
                                    CrosshairConfigStorage.save();
                                    return true;
                                }
                            }
                        }
                    }
                    y += rowHeight(i);
                }
            }
        }
        return super.mouseClicked(clickX, clickY, clickButton);
    }

    @Override
    public boolean mouseDragged(double clickX, double clickY, int clickButton, double dx, double dy) {
        if (pixelDrag(clickX, clickY, clickButton)) return true;
        for (SliderControl s : sliders) if (s != null && s.mouseDragged(clickX)) return true;
        return super.mouseDragged(clickX, clickY, clickButton, dx, dy);
    }

    @Override
    public boolean mouseReleased(double clickX, double clickY, int clickButton) {
        if (editing) { CrosshairConfigStorage.save(); return true; }
        boolean saved = false;
        for (SliderControl s : sliders) if (s != null) saved |= s.mouseReleased();
        if (saved) CrosshairConfigStorage.save();
        return super.mouseReleased(clickX, clickY, clickButton);
    }

    private boolean pixelClick(double mx, double my, int button) {
        if (editing) {
            if (com.blairdash.blueclient.mods.PixelCrosshair.click(width, height, mx, my, button) == 2) { editing = false; CrosshairConfigStorage.save(); }
            return true;
        }
        if (mod.id.equals("crosshair_opts") && button == 0 && Ui.inside(mx, my, com.blairdash.blueclient.mods.PixelCrosshair.editBtnX(width), doneY(), 90, 18)) {
            editing = true; com.blairdash.blueclient.mods.PixelCrosshair.open();
            return true;
        }
        return false;
    }

    private boolean pixelDrag(double mx, double my, int button) {
        if (!editing) return false;
        com.blairdash.blueclient.mods.PixelCrosshair.drag(width, height, mx, my, button);
        return true;
    }
    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (v * 14)));
        return true;
    }

    @Override
    public void close() {
        if (editing) { editing = false; CrosshairConfigStorage.save(); return; }
        CrosshairConfigStorage.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }
}
