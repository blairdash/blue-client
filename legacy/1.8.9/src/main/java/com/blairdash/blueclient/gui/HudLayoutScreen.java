package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.ConfigStorage;
import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.features.ArmorHud;
import com.blairdash.blueclient.features.CoordinatesHud;
import com.blairdash.blueclient.features.InfoHuds;
import com.blairdash.blueclient.features.Keystrokes;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Drag-and-drop HUD editor. The real HUD keeps drawing underneath; each
 * element gets an outlined box you can drag. Edges and the screen center snap.
 */
final class HudLayoutScreen extends GuiScreen {
    private static final int SNAP = 4;

    private static final class Box {
        final String name;
        final Supplier<Boolean> enabled;
        final Supplier<int[]> bounds;               // {x, y, w, h}
        final BiConsumer<Integer, Integer> mover;

        Box(String name, Supplier<Boolean> enabled, Supplier<int[]> bounds, BiConsumer<Integer, Integer> mover) {
            this.name = name;
            this.enabled = enabled;
            this.bounds = bounds;
            this.mover = mover;
        }

        int x() { return bounds.get()[0]; }
        int y() { return bounds.get()[1]; }
        int w() { return bounds.get()[2]; }
        int h() { return bounds.get()[3]; }
    }

    private final GuiScreen parent;
    private final List<Box> boxes = new ArrayList<Box>();
    private int dragging = -1;
    private int grabDX, grabDY;
    private boolean snappedX, snappedY;

    HudLayoutScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        boxes.clear();

        boxes.add(new Box("CPS / FPS", () -> ModConfig.cpsFpsHudEnabled,
                () -> new int[] { ModConfig.cpsFpsX, ModConfig.cpsFpsY, Ui.width("FPS: 999"), 20 },
                (x, y) -> { ModConfig.cpsFpsX = x; ModConfig.cpsFpsY = y; }));

        boxes.add(new Box("Coordinates", () -> ModConfig.coordinatesHudEnabled,
                () -> new int[] { CoordinatesHud.effectiveX(mc, width), CoordinatesHud.effectiveY(),
                        Ui.width("X: 0000  Y: 000  Z: 0000"), 10 },
                (x, y) -> { ModConfig.coordsX = x; ModConfig.coordsY = y; }));

        boxes.add(new Box("Potion HUD", () -> ModConfig.potionHudEnabled,
                () -> new int[] { ModConfig.potionHudX, ModConfig.potionHudY, 96, 30 },
                (x, y) -> { ModConfig.potionHudX = x; ModConfig.potionHudY = y; }));

        // Armor HUD: the stored position is the icon column; durability numbers sit to its left.
        final int armorLeft = 36;
        boxes.add(new Box("Armor HUD", () -> ModConfig.armorHudEnabled,
                () -> new int[] { ArmorHud.effectiveX(width) - armorLeft, ArmorHud.effectiveY(height), armorLeft + 16, ArmorHud.contentHeight() },
                (x, y) -> { ModConfig.armorHudX = x + armorLeft; ModConfig.armorHudY = y; }));

        boxes.add(new Box("Keystrokes", () -> ModConfig.keystrokesEnabled,
                () -> new int[] { Keystrokes.effectiveX(), Keystrokes.effectiveY(height), Keystrokes.width(), Keystrokes.height() },
                (x, y) -> { ModConfig.keystrokesX = x; ModConfig.keystrokesY = y; }));

        for (int i = 0; i < ModConfig.INFO_COUNT; i++) {
            final int id = i;
            boxes.add(new Box(InfoHuds.label(id), () -> ModConfig.infoEnabled[id],
                    () -> new int[] { InfoHuds.effectiveX(id, mc, width), InfoHuds.effectiveY(id, height),
                            Ui.width(InfoHuds.sample(id)), 10 },
                    (x, y) -> { ModConfig.infoX[id] = x; ModConfig.infoY[id] = y; }));
        }
        addExtraBoxes();
    }

    private void addExtraBoxes() {
        for (final com.blairdash.blueclient.features.ExtraMods.Mod m : com.blairdash.blueclient.features.ExtraMods.ALL) {
            if (!m.hasPosition()) continue;
            boxes.add(new Box(m.name, m::enabled,
                    () -> new int[] { m.x(width, mc), m.y(height), m.w(mc), m.h() },
                    (x, y) -> m.setPos(x, y)));
        }
    }

    private int doneX() { return width / 2 - 70; }
    private int resetX() { return width / 2 + 4; }
    private int btnY() { return height - 30; }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        Ui.fill(0, 0, width, height, 0x55050810);

        if (dragging >= 0) {
            if (snappedX) Ui.fill(width / 2, 0, width / 2 + 1, height, 0x663D8BFF);
            if (snappedY) Ui.fill(0, height / 2, width, height / 2 + 1, 0x663D8BFF);
        }

        int hover = hit(mx, my);
        for (int i = 0; i < boxes.size(); i++) {
            Box b = boxes.get(i);
            boolean on = b.enabled.get();
            boolean hot = i == dragging || (dragging < 0 && i == hover);
            int color = on ? Ui.ACCENT : Ui.DISABLED_TEXT;
            Ui.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), hot ? 0x553D8BFF : 0x223D8BFF);
            Ui.border(b.x() - 1, b.y() - 1, b.w() + 2, b.h() + 2, hot ? 0xFFFFFFFF : color);
            String label = b.name + (on ? "" : " (off)");
            int ly = b.y() >= 11 ? b.y() - 10 : b.y() + b.h() + 2;
            fontRendererObj.drawStringWithShadow(label, b.x(), ly, hot ? 0xFFFFFFFF : Ui.MUTED);
        }

        Ui.centered(fontRendererObj, "Drag an element to move it. Edges and the center snap.", width / 2, 8, 0xFFFFFFFF);

        boolean h1 = Ui.inside(mx, my, doneX(), btnY(), 66, 18);
        Ui.rrect(doneX(), btnY(), 66, 18, h1 ? Ui.ACCENT : Ui.ACCENT_DARK);
        Ui.label(fontRendererObj, "DONE", doneX(), btnY(), 66, 18, 0xFFFFFFFF);
        boolean h2 = Ui.inside(mx, my, resetX(), btnY(), 66, 18);
        Ui.rrect(resetX(), btnY(), 66, 18, h2 ? Ui.CARD_HOVER : Ui.CARD);
        Ui.label(fontRendererObj, "RESET", resetX(), btnY(), 66, 18, Ui.TEXT);
    }

    /** Topmost box under the point, or -1. */
    private int hit(int mx, int my) {
        for (int i = boxes.size() - 1; i >= 0; i--) {
            Box b = boxes.get(i);
            if (Ui.inside(mx, my, b.x() - 2, b.y() - 2, b.w() + 4, b.h() + 4)) return i;
        }
        return -1;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) throws IOException {
        if (button == 0) {
            if (Ui.inside(mx, my, doneX(), btnY(), 66, 18)) { close(); return; }
            if (Ui.inside(mx, my, resetX(), btnY(), 66, 18)) { resetAll(); return; }
            int i = hit(mx, my);
            if (i >= 0) {
                dragging = i;
                grabDX = mx - boxes.get(i).x();
                grabDY = my - boxes.get(i).y();
                return;
            }
        }
        super.mouseClicked(mx, my, button);
    }

    @Override
    protected void mouseClickMove(int mx, int my, int button, long time) {
        if (dragging < 0) return;
        Box b = boxes.get(dragging);
        int nx = mx - grabDX;
        int ny = my - grabDY;

        snappedX = false;
        snappedY = false;
        int[] xs = { 0, width - b.w(), (width - b.w()) / 2 };
        for (int i = 0; i < xs.length; i++) {
            if (Math.abs(nx - xs[i]) <= SNAP) { nx = xs[i]; snappedX = i == 2; break; }
        }
        int[] ys = { 0, height - b.h(), (height - b.h()) / 2 };
        for (int i = 0; i < ys.length; i++) {
            if (Math.abs(ny - ys[i]) <= SNAP) { ny = ys[i]; snappedY = i == 2; break; }
        }

        nx = Math.max(0, Math.min(nx, Math.max(0, width - b.w())));
        ny = Math.max(0, Math.min(ny, Math.max(0, height - b.h())));
        b.mover.accept(nx, ny);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        if (dragging >= 0) {
            dragging = -1;
            snappedX = false;
            snappedY = false;
            ConfigStorage.save();
            return;
        }
        super.mouseReleased(mx, my, state);
    }

    private void resetAll() {
        ModConfig.cpsFpsX = 6;
        ModConfig.cpsFpsY = 6;
        ModConfig.potionHudX = 6;
        ModConfig.potionHudY = 34;
        ModConfig.coordsX = -1;
        ModConfig.coordsY = -1;
        ModConfig.armorHudX = -1;
        ModConfig.armorHudY = -1;
        ModConfig.keystrokesX = -1;
        ModConfig.keystrokesY = -1;
        java.util.Arrays.fill(ModConfig.infoX, -1);
        java.util.Arrays.fill(ModConfig.infoY, -1);
        ModConfig.extraX.clear();
        ModConfig.extraY.clear();
        ConfigStorage.save();
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
