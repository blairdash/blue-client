package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.features.ArmorHud;
import com.blairdash.blueclient.features.CoordinatesHud;
import com.blairdash.blueclient.features.InfoHuds;
import com.blairdash.blueclient.features.Keystrokes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Drag-and-drop HUD editor. The real HUD keeps drawing underneath; each
 * element gets an outlined box you can drag. Edges and the screen center snap.
 */
final class HudLayoutScreen extends Screen {
    private static final int SNAP = 4;

    private interface Box {
        String name();
        boolean enabled();
        int x();
        int y();
        int w();
        int h();
        /** Moves the element so its box's top-left is at (x, y). */
        void moveTo(int x, int y);
    }

    private static final class SimpleBox implements Box {
        private final String name;
        private final Supplier<Boolean> enabled;
        private final Supplier<int[]> bounds;           // {x, y, w, h}
        private final BiConsumer<Integer, Integer> mover;

        SimpleBox(String name, Supplier<Boolean> enabled, Supplier<int[]> bounds, BiConsumer<Integer, Integer> mover) {
            this.name = name;
            this.enabled = enabled;
            this.bounds = bounds;
            this.mover = mover;
        }

        public String name() { return name; }
        public boolean enabled() { return enabled.get(); }
        public int x() { return bounds.get()[0]; }
        public int y() { return bounds.get()[1]; }
        public int w() { return bounds.get()[2]; }
        public int h() { return bounds.get()[3]; }
        public void moveTo(int x, int y) { mover.accept(x, y); }
    }

    private final Screen parent;
    private final List<Box> boxes = new ArrayList<>();
    private int dragging = -1;
    private int grabDX, grabDY;
    private boolean snappedX, snappedY;

    HudLayoutScreen(Screen parent) {
        super(Component.literal("HUD Layout"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        boxes.clear();
        Minecraft mc = Minecraft.getInstance();

        boxes.add(new SimpleBox("CPS / FPS", () -> ModConfig.cpsFpsHudEnabled,
                () -> new int[] { ModConfig.cpsFpsX, ModConfig.cpsFpsY, font.width("FPS: 999"), 20 },
                (x, y) -> { ModConfig.cpsFpsX = x; ModConfig.cpsFpsY = y; }));

        boxes.add(new SimpleBox("Coordinates", () -> ModConfig.coordinatesHudEnabled,
                () -> new int[] { CoordinatesHud.effectiveX(mc, width), CoordinatesHud.effectiveY(),
                        font.width("X: 0000  Y: 000  Z: 0000"), 10 },
                (x, y) -> { ModConfig.coordsX = x; ModConfig.coordsY = y; }));

        boxes.add(new SimpleBox("Potion HUD", () -> ModConfig.potionHudEnabled,
                () -> new int[] { ModConfig.potionHudX, ModConfig.potionHudY, 96, 30 },
                (x, y) -> { ModConfig.potionHudX = x; ModConfig.potionHudY = y; }));

        // Armor HUD: the stored position is the icon column; durability numbers sit to its left.
        final int armorLeft = 36;
        boxes.add(new SimpleBox("Armor HUD", () -> ModConfig.armorHudEnabled,
                () -> new int[] { ArmorHud.effectiveX(width) - armorLeft, ArmorHud.effectiveY(height), armorLeft + 16, ArmorHud.contentHeight() },
                (x, y) -> { ModConfig.armorHudX = x + armorLeft; ModConfig.armorHudY = y; }));

        boxes.add(new SimpleBox("Keystrokes", () -> ModConfig.keystrokesEnabled,
                () -> new int[] { Keystrokes.effectiveX(), Keystrokes.effectiveY(height), Keystrokes.width(), Keystrokes.height() },
                (x, y) -> { ModConfig.keystrokesX = x; ModConfig.keystrokesY = y; }));

        for (int i = 0; i < ModConfig.INFO_COUNT; i++) {
            final int id = i;
            boxes.add(new SimpleBox(InfoHuds.label(id), () -> ModConfig.infoEnabled[id],
                    () -> new int[] { InfoHuds.effectiveX(id, Minecraft.getInstance(), width), InfoHuds.effectiveY(id, height),
                            font.width(InfoHuds.sample(id)), 10 },
                    (x, y) -> { ModConfig.infoX[id] = x; ModConfig.infoY[id] = y; }));
        }
        for (com.blairdash.blueclient.mods.BlueMod bm : com.blairdash.blueclient.mods.ModManager.all()) {
            if (!bm.isHud()) continue;
            boxes.add(new SimpleBox(bm.name, () -> bm.enabled,
                    () -> new int[] { bm.x(mc, width, height), bm.y(mc, width, height), bm.hudW(mc), bm.hudH(mc) },
                    (x, y) -> { bm.hudX = x; bm.hudY = y; }));
        }
    }

    private int doneX() { return width / 2 - 70; }
    private int resetX() { return width / 2 + 4; }
    private int btnY() { return height - 30; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor c, int mx, int my, float delta) {
        super.extractRenderState(c, mx, my, delta);
        c.fill(0, 0, width, height, 0x55050810);

        // center guides while dragging
        if (dragging >= 0) {
            if (snappedX) c.fill(width / 2, 0, width / 2 + 1, height, 0x663D8BFF);
            if (snappedY) c.fill(0, height / 2, width, height / 2 + 1, 0x663D8BFF);
        }

        int hover = hit(mx, my);
        for (int i = 0; i < boxes.size(); i++) {
            Box b = boxes.get(i);
            boolean active = i == dragging;
            boolean hot = active || (dragging < 0 && i == hover);
            int color = b.enabled() ? Ui.ACCENT : Ui.DISABLED_TEXT;
            c.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), hot ? 0x553D8BFF : 0x223D8BFF);
            Ui.border(c, b.x() - 1, b.y() - 1, b.w() + 2, b.h() + 2, hot ? 0xFFFFFFFF : color);
            String label = b.name() + (b.enabled() ? "" : " (off)");
            int ly = b.y() >= 11 ? b.y() - 10 : b.y() + b.h() + 2;
            c.text(font, label, b.x(), ly, hot ? 0xFFFFFFFF : Ui.MUTED, true);
        }

        Ui.centered(c, font, "Drag an element to move it. Edges and the center snap.", width / 2, 8, 0xFFFFFFFF);

        boolean h1 = Ui.inside(mx, my, doneX(), btnY(), 66, 18);
        Ui.rrect(c, doneX(), btnY(), 66, 18, h1 ? Ui.ACCENT : Ui.ACCENT_DARK);
        Ui.label(c, font, "DONE", doneX(), btnY(), 66, 18, 0xFFFFFFFF);
        boolean h2 = Ui.inside(mx, my, resetX(), btnY(), 66, 18);
        Ui.rrect(c, resetX(), btnY(), 66, 18, h2 ? Ui.CARD_HOVER : Ui.CARD);
        Ui.label(c, font, "RESET", resetX(), btnY(), 66, 18, Ui.TEXT);
    }

    /** Topmost box under the point, or -1. */
    private int hit(double mx, double my) {
        for (int i = boxes.size() - 1; i >= 0; i--) {
            Box b = boxes.get(i);
            if (Ui.inside(mx, my, b.x() - 2, b.y() - 2, b.w() + 4, b.h() + 4)) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (click.button() == 0) {
            if (Ui.inside(mx, my, doneX(), btnY(), 66, 18)) { onClose(); return true; }
            if (Ui.inside(mx, my, resetX(), btnY(), 66, 18)) { resetAll(); return true; }
            int i = hit(mx, my);
            if (i >= 0) {
                dragging = i;
                grabDX = (int) mx - boxes.get(i).x();
                grabDY = (int) my - boxes.get(i).y();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (dragging < 0) return super.mouseDragged(click, dx, dy);
        Box b = boxes.get(dragging);
        int nx = (int) click.x() - grabDX;
        int ny = (int) click.y() - grabDY;

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
        b.moveTo(nx, ny);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging >= 0) {
            dragging = -1;
            snappedX = false;
            snappedY = false;
            CrosshairConfigStorage.save();
            return true;
        }
        return super.mouseReleased(click);
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
        com.blairdash.blueclient.mods.ModManager.resetHudPositions();
        CrosshairConfigStorage.save();
    }

    @Override
    public void onClose() {
        CrosshairConfigStorage.save();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
