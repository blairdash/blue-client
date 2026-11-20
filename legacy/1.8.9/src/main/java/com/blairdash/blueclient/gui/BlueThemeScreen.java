package com.blairdash.blueclient.gui;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

import java.io.IOException;

/** "Select Theme" popup with a settings gear (Main Menu Style: Blue / Vanilla). */
public class BlueThemeScreen extends GuiScreen {
    private final GuiScreen parent;
    private boolean settings;

    public BlueThemeScreen(GuiScreen parent) { this.parent = parent; }

    private int pw() { return 420; }
    private int ph() { return 210; }
    private int px() { return (width - pw()) / 2; }
    private int py() { return (height - ph()) / 2; }
    private int cardW() { return (pw() - 40 - 24) / 3; }
    private int cardX(int i) { return px() + 20 + i * (cardW() + 12); }
    private int cardY() { return py() + 62; }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        if (parent != null) parent.drawScreen(-1, -1, pt);
        Ui.fill(0, 0, width, height, 0xAA05070D);
        int x = px(), y = py();
        Ui.card(x, y, pw(), ph(), 8, Ui.PANEL, Ui.PANEL_EDGE);
        Ui.text(fontRendererObj, "Select Theme", x + 20, y + 18, Ui.TEXT);

        boolean gh = Ui.inside(mx, my, x + pw() - 56, y + 12, 18, 18), xh = Ui.inside(mx, my, x + pw() - 32, y + 12, 18, 18);
        Ui.rrect(x + pw() - 56, y + 12, 18, 18, 4, gh ? Ui.CARD_HOVER : Ui.CARD);
        Ui.label(fontRendererObj, "*", x + pw() - 56, y + 12, 18, 18, Ui.TEXT);
        Ui.rrect(x + pw() - 32, y + 12, 18, 18, 4, xh ? Ui.BAD : Ui.CARD);
        Ui.label(fontRendererObj, "x", x + pw() - 32, y + 12, 18, 18, Ui.TEXT);

        Ui.text(fontRendererObj, "Themes (" + BlueMenuConfig.THEMES.length + ")", x + 20, y + 44, Ui.MUTED);
        int active = BlueMenuConfig.theme();
        for (int i = 0; i < BlueMenuConfig.THEMES.length; i++) {
            int cx = cardX(i), cy = cardY(), cw = cardW(), chh = 70;
            boolean hov = Ui.inside(mx, my, cx, cy, cw, chh + 18);
            Ui.card(cx - 1, cy - 1, cw + 2, chh + 2, 5, i == active ? Ui.ACCENT : (hov ? Ui.MUTED : Ui.PANEL_EDGE), i == active ? Ui.ACCENT : Ui.PANEL_EDGE);
            BlueTitleScreen.paintTheme(i, cx, cy, cx + cw, cy + chh);
            Ui.text(fontRendererObj, BlueMenuConfig.THEMES[i], cx, cy + chh + 6, Ui.TEXT);
            Ui.rrect(cx + cw - 10, cy + chh + 8, 8, 8, 4, i == active ? Ui.GOOD : Ui.TRACK_OFF);
        }
        Ui.text(fontRendererObj, "Active: " + BlueMenuConfig.THEMES[active], x + 20, y + ph() - 22, Ui.TEXT);

        if (settings) {
            Ui.fill(0, 0, width, height, 0x88000000);
            int sw = 300, sh = 140, sx = (width - sw) / 2, sy = (height - sh) / 2;
            Ui.card(sx, sy, sw, sh, 8, Ui.PANEL, Ui.PANEL_EDGE);
            Ui.text(fontRendererObj, "Settings", sx + 16, sy + 14, Ui.TEXT);
            Ui.text(fontRendererObj, "Main Menu Style", sx + 16, sy + 36, Ui.TEXT);
            radio(mx, my, sx + 16, sy + 54, "Blue Client", "The Blue Client main menu", BlueMenuConfig.blueMenu());
            radio(mx, my, sx + 16, sy + 86, "Vanilla", "The normal Minecraft title screen", !BlueMenuConfig.blueMenu());
            boolean ch = Ui.inside(mx, my, sx + sw - 30, sy + 10, 18, 18);
            Ui.rrect(sx + sw - 30, sy + 10, 18, 18, 4, ch ? Ui.BAD : Ui.CARD);
            Ui.label(fontRendererObj, "x", sx + sw - 30, sy + 10, 18, 18, Ui.TEXT);
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void radio(int mx, int my, int x, int y, String name, String desc, boolean on) {
        boolean hov = Ui.inside(mx, my, x, y, 268, 28);
        Ui.rrect(x, y + 2, 10, 10, 5, on ? Ui.ACCENT : (hov ? Ui.MUTED : Ui.TRACK_OFF));
        if (on) Ui.rrect(x + 3, y + 5, 4, 4, 2, 0xFFFFFFFF);
        Ui.text(fontRendererObj, name, x + 18, y + 2, Ui.TEXT);
        Ui.text(fontRendererObj, desc, x + 18, y + 14, Ui.MUTED);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button != 0) { super.mouseClicked(mx, my, button); return; }
        int x = px(), y = py();
        if (settings) {
            int sw = 300, sh = 140, sx = (width - sw) / 2, sy = (height - sh) / 2;
            if (Ui.inside(mx, my, sx + sw - 30, sy + 10, 18, 18) || !Ui.inside(mx, my, sx, sy, sw, sh)) { settings = false; return; }
            if (Ui.inside(mx, my, sx + 16, sy + 54, 268, 28)) {
                boolean wasVanilla = !BlueMenuConfig.blueMenu();
                BlueMenuConfig.setBlueMenu(true);
                if (wasVanilla) { mc.displayGuiScreen(new BlueTitleScreen()); return; }
            }
            if (Ui.inside(mx, my, sx + 16, sy + 86, 268, 28)) {
                BlueMenuConfig.setBlueMenu(false);
                mc.displayGuiScreen(new GuiMainMenu());
                return;
            }
            return;
        }
        if (Ui.inside(mx, my, x + pw() - 32, y + 12, 18, 18) || !Ui.inside(mx, my, x, y, pw(), ph())) { close(); return; }
        if (Ui.inside(mx, my, x + pw() - 56, y + 12, 18, 18)) { settings = true; return; }
        for (int i = 0; i < BlueMenuConfig.THEMES.length; i++) {
            if (Ui.inside(mx, my, cardX(i), cardY(), cardW(), 88)) { BlueMenuConfig.setTheme(i); return; }
        }
    }

    private void close() { mc.displayGuiScreen(parent); }

    @Override
    protected void keyTyped(char typed, int keyCode) throws IOException {
        if (keyCode == 1) { if (settings) settings = false; else close(); return; }
        super.keyTyped(typed, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
