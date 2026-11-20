package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.features.ArmorHud;
import com.blairdash.blueclient.features.CoordinatesHud;
import com.blairdash.blueclient.features.Keystrokes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Set the X/Y position of the CPS/FPS display, Potion HUD, Coordinates, Armor HUD, and Keystrokes. */
public class HudPositionScreen extends Screen {
    private final Screen parent;

    private EditBox cpsX, cpsY, potionX, potionY, coordsX, coordsY, armorX, armorY, keysX, keysY;
    private int xColumn, yColumn;
    private int coordsDefaultX, coordsDefaultY, armorDefaultX, armorDefaultY, keysDefaultX, keysDefaultY;

    public HudPositionScreen(Screen parent) {
        super(Component.literal("HUD Positions"));
        this.parent = parent;
    }

    private static int rowY(int row) {
        return 54 + row * 24;
    }

    @Override
    protected void init() {
        Minecraft client = Minecraft.getInstance();
        int centerX = width / 2;
        xColumn = centerX - 30;
        yColumn = centerX + 40;

        cpsX = field(xColumn, rowY(0), ModConfig.cpsFpsX);
        cpsY = field(yColumn, rowY(0), ModConfig.cpsFpsY);

        potionX = field(xColumn, rowY(1), ModConfig.potionHudX);
        potionY = field(yColumn, rowY(1), ModConfig.potionHudY);

        coordsDefaultX = CoordinatesHud.effectiveX(client, width);
        coordsDefaultY = CoordinatesHud.effectiveY();
        coordsX = field(xColumn, rowY(2), coordsDefaultX);
        coordsY = field(yColumn, rowY(2), coordsDefaultY);

        armorDefaultX = ArmorHud.effectiveX(width);
        armorDefaultY = ArmorHud.effectiveY(height);
        armorX = field(xColumn, rowY(3), armorDefaultX);
        armorY = field(yColumn, rowY(3), armorDefaultY);

        keysDefaultX = Keystrokes.effectiveX();
        keysDefaultY = Keystrokes.effectiveY(height);
        keysX = field(xColumn, rowY(4), keysDefaultX);
        keysY = field(yColumn, rowY(4), keysDefaultY);

        int buttonsY = rowY(4) + 32;
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAndClose())
                .bounds(centerX - 126, buttonsY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> resetToDefaults())
                .bounds(centerX - 40, buttonsY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(centerX + 46, buttonsY, 80, 20).build());
    }

    private EditBox field(int x, int y, int value) {
        EditBox f = new EditBox(font, x, y, 60, 20, Component.literal(""));
        f.setMaxLength(5);
        f.setValue(String.valueOf(value));
        addRenderableWidget(f);
        return f;
    }

    private static int parse(EditBox f, int fallback, int max) {
        try {
            int v = Integer.parseInt(f.getValue().trim());
            return Math.max(0, Math.min(v, max));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void saveAndClose() {
        ModConfig.cpsFpsX = parse(cpsX, ModConfig.cpsFpsX, width - 1);
        ModConfig.cpsFpsY = parse(cpsY, ModConfig.cpsFpsY, height - 1);
        ModConfig.potionHudX = parse(potionX, ModConfig.potionHudX, width - 1);
        ModConfig.potionHudY = parse(potionY, ModConfig.potionHudY, height - 1);

        // Coordinates, Armor HUD and Keystrokes: if still on the automatic default and the
        // value wasn't changed, keep it automatic instead of freezing it.
        int cx = parse(coordsX, coordsDefaultX, width - 1);
        int cy = parse(coordsY, coordsDefaultY, height - 1);
        ModConfig.coordsX = (ModConfig.coordsX < 0 && cx == coordsDefaultX) ? -1 : cx;
        ModConfig.coordsY = (ModConfig.coordsY < 0 && cy == coordsDefaultY) ? -1 : cy;

        int ax = parse(armorX, armorDefaultX, width - 1);
        int ay = parse(armorY, armorDefaultY, height - 1);
        ModConfig.armorHudX = (ModConfig.armorHudX < 0 && ax == armorDefaultX) ? -1 : ax;
        ModConfig.armorHudY = (ModConfig.armorHudY < 0 && ay == armorDefaultY) ? -1 : ay;

        int kx = parse(keysX, keysDefaultX, width - 1);
        int ky = parse(keysY, keysDefaultY, height - 1);
        ModConfig.keystrokesX = (ModConfig.keystrokesX < 0 && kx == keysDefaultX) ? -1 : kx;
        ModConfig.keystrokesY = (ModConfig.keystrokesY < 0 && ky == keysDefaultY) ? -1 : ky;

        CrosshairConfigStorage.save();
        onClose();
    }

    private void resetToDefaults() {
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
        CrosshairConfigStorage.save();
        clearWidgets();
        init();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int centerX = width / 2;
        context.centeredText(font, title, centerX, 14, 0xFFFFFFFF);

        context.centeredText(font, "X", xColumn + 30, 40, 0xFFAAAAAA);
        context.centeredText(font, "Y", yColumn + 30, 40, 0xFFAAAAAA);

        int labelX = centerX - 110;
        context.text(font, "CPS / FPS", labelX, rowY(0) + 6, 0xFFFFFFFF);
        context.text(font, "Potion HUD", labelX, rowY(1) + 6, 0xFFFFFFFF);
        context.text(font, "Coordinates", labelX, rowY(2) + 6, 0xFFFFFFFF);
        context.text(font, "Armor HUD", labelX, rowY(3) + 6, 0xFFFFFFFF);
        context.text(font, "Keystrokes", labelX, rowY(4) + 6, 0xFFFFFFFF);

        int noteY = rowY(4) + 58;
        context.centeredText(font, "X / Y = pixels from the top-left of the screen.", centerX, noteY, 0xFFAAAAAA);
        context.centeredText(font, "Armor HUD / Keystrokes: top-left of the display.", centerX, noteY + 11, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}