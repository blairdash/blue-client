package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.ConfigStorage;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

/** A small options panel with one or more sliders (Zoom FOV, Low Fire offset, ...). */
final class SliderOptionsScreen extends GuiScreen {
    private final GuiScreen parent;
    private final String modName;
    private final SliderControl[] sliders;

    SliderOptionsScreen(GuiScreen parent, String modName, SliderControl... sliders) {
        this.parent = parent;
        this.modName = modName;
        this.sliders = sliders;
    }

    private int pw() { return Math.min(width - 20, 300); }
    private int ph() { return 64 + sliders.length * (SliderControl.HEIGHT + 10) + 28; }
    private int px() { return (width - pw()) / 2; }
    private int py() { return (height - ph()) / 2; }
    private int doneX() { return px() + pw() - 76; }
    private int doneY() { return py() + ph() - 28; }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        Ui.fill(0, 0, width, height, Ui.DIM);
        int px = px(), py = py(), pw = pw(), ph = ph();
        Ui.rrect(px - 1, py - 1, pw + 2, ph + 2, Ui.PANEL_EDGE);
        Ui.rrect(px, py, pw, ph, Ui.PANEL);
        Ui.fill(px, py, px + pw, py + 26, Ui.HEADER);
        Ui.text(fontRendererObj, modName.toUpperCase(), px + 10, py + 9, Ui.TEXT);

        int y = py + 40;
        for (SliderControl s : sliders) {
            s.place(px + 14, y, pw - 28);
            s.render(fontRendererObj, mx, my);
            y += SliderControl.HEIGHT + 10;
        }

        boolean hover = Ui.inside(mx, my, doneX(), doneY(), 66, 18);
        Ui.rrect(doneX(), doneY(), 66, 18, hover ? Ui.ACCENT : Ui.ACCENT_DARK);
        Ui.label(fontRendererObj, "DONE", doneX(), doneY(), 66, 18, 0xFFFFFFFF);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button == 0) {
            for (SliderControl s : sliders) if (s.mouseClicked(mx, my)) return;
            if (Ui.inside(mx, my, doneX(), doneY(), 66, 18)) { close(); return; }
        }
        super.mouseClicked(mx, my, button);
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        for (SliderControl s : sliders) if (s.mouseDragged(mx)) return;
        super.mouseClickMove(mx, my, button, time);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        boolean saved = false;
        for (SliderControl s : sliders) saved |= s.mouseReleased();
        if (saved) ConfigStorage.save();
        super.mouseReleased(mx, my, state);
    }

    @Override
    protected void keyTyped(char typed, int keyCode) throws IOException {
        if (keyCode == 1) { close(); return; }
        super.keyTyped(typed, keyCode);
    }

    private void close() {
        ConfigStorage.save();
        this.mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
