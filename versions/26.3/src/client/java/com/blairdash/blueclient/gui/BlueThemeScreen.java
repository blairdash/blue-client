package com.blairdash.blueclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** "Select Theme" popup with a settings gear (Main Menu Style: Blue / Vanilla). */
public class BlueThemeScreen extends BlueScreen {
    private final Screen parent;
    private boolean settings;

    public BlueThemeScreen(Screen parent) { super(Component.literal("Select Theme")); this.parent = parent; }

    private int pw() { return 420; }
    private int ph() { return 210; }
    private int px() { return (width - pw()) / 2; }
    private int py() { return (height - ph()) / 2; }
    private int cardW() { return (pw() - 40 - 24) / 3; }
    private int cardX(int i) { return px() + 20 + i * (cardW() + 12); }
    private int cardY() { return py() + 62; }

    @Override
    protected void draw(GuiGraphicsExtractor c, int mx, int my, float delta) {
        BlueScreen.drawChild(parent, c, -1, -1, delta);
        c.fill(0, 0, width, height, 0xAA05070D);
        int x = px(), y = py();
        Ui.card(c, x, y, pw(), ph(), 8, Ui.PANEL, Ui.PANEL_EDGE);
        Ui.text(c, font, "Select Theme", x + 20, y + 18, Ui.TEXT);

        boolean gh = Ui.inside(mx, my, x + pw() - 56, y + 12, 18, 18), xh = Ui.inside(mx, my, x + pw() - 32, y + 12, 18, 18);
        Ui.rrect(c, x + pw() - 56, y + 12, 18, 18, 4, gh ? Ui.CARD_HOVER : Ui.CARD);
        Ui.label(c, font, "*", x + pw() - 56, y + 12, 18, 18, Ui.TEXT);
        Ui.rrect(c, x + pw() - 32, y + 12, 18, 18, 4, xh ? Ui.BAD : Ui.CARD);
        Ui.label(c, font, "x", x + pw() - 32, y + 12, 18, 18, Ui.TEXT);

        Ui.text(c, font, "Themes (" + BlueMenuConfig.THEMES.length + ")", x + 20, y + 44, Ui.MUTED);
        int active = BlueMenuConfig.theme();
        for (int i = 0; i < BlueMenuConfig.THEMES.length; i++) {
            int cx = cardX(i), cy = cardY(), cw = cardW(), chh = 70;
            boolean hov = Ui.inside(mx, my, cx, cy, cw, chh + 18);
            Ui.card(c, cx - 1, cy - 1, cw + 2, chh + 2, 5, i == active ? Ui.ACCENT : (hov ? Ui.MUTED : Ui.PANEL_EDGE), i == active ? Ui.ACCENT : Ui.PANEL_EDGE);
            BlueTitleScreen.paintTheme(c, i, cx, cy, cx + cw, cy + chh);
            Ui.text(c, font, BlueMenuConfig.THEMES[i], cx, cy + chh + 6, Ui.TEXT);
            Ui.rrect(c, cx + cw - 10, cy + chh + 8, 8, 8, 4, i == active ? Ui.GOOD : Ui.TRACK_OFF);
        }
        Ui.text(c, font, "Active: " + BlueMenuConfig.THEMES[active], x + 20, y + ph() - 22, Ui.TEXT);

        if (settings) {
            c.fill(0, 0, width, height, 0x88000000);
            int sw = 300, sh = 140, sx = (width - sw) / 2, sy = (height - sh) / 2;
            Ui.card(c, sx, sy, sw, sh, 8, Ui.PANEL, Ui.PANEL_EDGE);
            Ui.text(c, font, "Settings", sx + 16, sy + 14, Ui.TEXT);
            Ui.text(c, font, "Main Menu Style", sx + 16, sy + 36, Ui.TEXT);
            radio(c, mx, my, sx + 16, sy + 54, "Blue Client", "The Blue Client main menu", BlueMenuConfig.blueMenu());
            radio(c, mx, my, sx + 16, sy + 86, "Vanilla", "The normal Minecraft title screen", !BlueMenuConfig.blueMenu());
            boolean ch = Ui.inside(mx, my, sx + sw - 30, sy + 10, 18, 18);
            Ui.rrect(c, sx + sw - 30, sy + 10, 18, 18, 4, ch ? Ui.BAD : Ui.CARD);
            Ui.label(c, font, "x", sx + sw - 30, sy + 10, 18, 18, Ui.TEXT);
        }
    }

    private void radio(GuiGraphicsExtractor c, int mx, int my, int x, int y, String name, String desc, boolean on) {
        boolean hov = Ui.inside(mx, my, x, y, 268, 28);
        Ui.rrect(c, x, y + 2, 10, 10, 5, on ? Ui.ACCENT : (hov ? Ui.MUTED : Ui.TRACK_OFF));
        if (on) Ui.rrect(c, x + 3, y + 5, 4, 4, 2, 0xFFFFFFFF);
        Ui.text(c, font, name, x + 18, y + 2, Ui.TEXT);
        Ui.text(c, font, desc, x + 18, y + 14, Ui.MUTED);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(click, doubled);
        double mx = click.x(), my = click.y();
        int x = px(), y = py();
        if (settings) {
            int sw = 300, sh = 140, sx = (width - sw) / 2, sy = (height - sh) / 2;
            if (Ui.inside(mx, my, sx + sw - 30, sy + 10, 18, 18) || !Ui.inside(mx, my, sx, sy, sw, sh)) { settings = false; return true; }
            if (Ui.inside(mx, my, sx + 16, sy + 54, 268, 28)) { boolean wasVanilla = !BlueMenuConfig.blueMenu(); BlueMenuConfig.setBlueMenu(true); if (wasVanilla) { net.minecraft.client.Minecraft.getInstance().gui.setScreen(new BlueTitleScreen()); return true; } }
            if (Ui.inside(mx, my, sx + 16, sy + 86, 268, 28)) { BlueMenuConfig.setBlueMenu(false); net.minecraft.client.Minecraft.getInstance().gui.setScreen(new net.minecraft.client.gui.screens.TitleScreen()); return true; }
            return true;
        }
        if (Ui.inside(mx, my, x + pw() - 32, y + 12, 18, 18) || !Ui.inside(mx, my, x, y, pw(), ph())) { onClose(); return true; }
        if (Ui.inside(mx, my, x + pw() - 56, y + 12, 18, 18)) { settings = true; return true; }
        for (int i = 0; i < BlueMenuConfig.THEMES.length; i++) {
            if (Ui.inside(mx, my, cardX(i), cardY(), cardW(), 88)) { BlueMenuConfig.setTheme(i); return true; }
        }
        return true;
    }

    @Override
    public void onClose() { ScreenNav.open(parent); }

    @Override
    public boolean isPauseScreen() { return false; }
}
