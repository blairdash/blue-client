package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** A small options panel with one or more sliders (Zoom FOV, Low Fire offset, ...). */
final class SliderOptionsScreen extends Screen {
    private final Screen parent;
    private final String modName;
    private final SliderControl[] sliders;

    SliderOptionsScreen(Screen parent, String modName, SliderControl... sliders) {
        super(Text.literal(modName + " options"));
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
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        c.fill(0, 0, width, height, Ui.DIM);
        int px = px(), py = py(), pw = pw(), ph = ph();
        Ui.rrect(c, px - 1, py - 1, pw + 2, ph + 2, Ui.PANEL_EDGE);
        Ui.rrect(c, px, py, pw, ph, Ui.PANEL);
        c.fill(px, py, px + pw, py + 26, Ui.HEADER);
        c.drawText(textRenderer, modName.toUpperCase(), px + 10, py + 9, Ui.TEXT, false);

        int y = py + 40;
        for (SliderControl s : sliders) {
            s.place(px + 14, y, pw - 28);
            s.render(c, textRenderer, mx, my);
            y += SliderControl.HEIGHT + 10;
        }

        boolean hover = Ui.inside(mx, my, doneX(), doneY(), 66, 18);
        Ui.rrect(c, doneX(), doneY(), 66, 18, hover ? Ui.ACCENT : Ui.ACCENT_DARK);
        Ui.label(c, textRenderer, "DONE", doneX(), doneY(), 66, 18, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx0, double my0, int button0) {
        double mx = mx0, my = my0;
        if (button0 == 0) {
            for (SliderControl s : sliders) if (s.mouseClicked(mx, my)) return true;
            if (Ui.inside(mx, my, doneX(), doneY(), 66, 18)) { close(); return true; }
        }
        return super.mouseClicked(mx0, my0, button0);
    }

    @Override
    public boolean mouseDragged(double mx0, double my0, int button0, double dx, double dy) {
        for (SliderControl s : sliders) if (s.mouseDragged(mx0)) return true;
        return super.mouseDragged(mx0, my0, button0, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx0, double my0, int button0) {
        boolean saved = false;
        for (SliderControl s : sliders) saved |= s.mouseReleased();
        if (saved) CrosshairConfigStorage.save();
        return super.mouseReleased(mx0, my0, button0);
    }

    @Override
    public void close() {
        CrosshairConfigStorage.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
