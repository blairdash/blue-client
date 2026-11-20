package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CrosshairSettingsScreen extends Screen {
    private final Screen parent;

    private EditBox colorField;
    private EditBox sizeField;
    private EditBox thicknessField;
    private EditBox gapField;
    private EditBox enemyColorField;

    private boolean enemyEnabled;
    private Button enemyToggleButton;

    public CrosshairSettingsScreen(Screen parent) {
        super(Component.literal("Custom Crosshair"));
        this.parent = parent;
        this.enemyEnabled = ModConfig.enemyCrosshairEnabled;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int y = 50;
        int fieldWidth = 200;

        colorField = new EditBox(font, centerX - fieldWidth / 2, y, fieldWidth, 20, Component.literal("Color"));
        colorField.setMaxLength(8);
        colorField.setValue(String.format("%08X", ModConfig.crosshairColor));
        addRenderableWidget(colorField);
        y += 28;

        sizeField = new EditBox(font, centerX - fieldWidth / 2, y, fieldWidth, 20, Component.literal("Size"));
        sizeField.setValue(String.valueOf((int) ModConfig.crosshairSize));
        addRenderableWidget(sizeField);
        y += 28;

        thicknessField = new EditBox(font, centerX - fieldWidth / 2, y, fieldWidth, 20, Component.literal("Thickness"));
        thicknessField.setValue(String.valueOf((int) ModConfig.crosshairThickness));
        addRenderableWidget(thicknessField);
        y += 28;

        gapField = new EditBox(font, centerX - fieldWidth / 2, y, fieldWidth, 20, Component.literal("Gap"));
        gapField.setValue(String.valueOf((int) ModConfig.crosshairGap));
        addRenderableWidget(gapField);
        y += 36;

        enemyToggleButton = Button.builder(enemyToggleText(), b -> {
            enemyEnabled = !enemyEnabled;
            b.setMessage(enemyToggleText());
        }).bounds(centerX - fieldWidth / 2, y, fieldWidth, 20).build();
        addRenderableWidget(enemyToggleButton);
        y += 28;

        enemyColorField = new EditBox(font, centerX - fieldWidth / 2, y, fieldWidth, 20, Component.literal("Enemy Color"));
        enemyColorField.setMaxLength(8);
        enemyColorField.setValue(String.format("%08X", ModConfig.enemyCrosshairColor));
        addRenderableWidget(enemyColorField);
        y += 36;

        addRenderableWidget(Button.builder(Component.literal("More options (dot, outline, ...)"),
                b -> Minecraft.getInstance().setScreen(new GenericOptionsScreen(this, com.blairdash.blueclient.mods.ModManager.get("crosshair_opts"))))
                .bounds(centerX - fieldWidth / 2, y - 28, fieldWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAndClose())
                .bounds(centerX - 104, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(centerX + 4, y, 100, 20).build());
    }

    private Component enemyToggleText() {
        return Component.literal("Enemy Crosshair: " + (enemyEnabled ? "ON" : "OFF"));
    }

    private void saveAndClose() {
        try {
            ModConfig.crosshairColor = (int) Long.parseLong(colorField.getValue().trim(), 16);
        } catch (NumberFormatException ignored) { }
        try {
            ModConfig.enemyCrosshairColor = (int) Long.parseLong(enemyColorField.getValue().trim(), 16);
        } catch (NumberFormatException ignored) { }
        ModConfig.crosshairSize = parseFloatSafe(sizeField.getValue(), ModConfig.crosshairSize);
        ModConfig.crosshairThickness = parseFloatSafe(thicknessField.getValue(), ModConfig.crosshairThickness);
        ModConfig.crosshairGap = parseFloatSafe(gapField.getValue(), ModConfig.crosshairGap);
        ModConfig.enemyCrosshairEnabled = enemyEnabled;
        CrosshairConfigStorage.save();
        onClose();
    }

    private static float parseFloatSafe(String text, float fallback) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, Component.literal("Colors are ARGB hex, e.g. FFFFFFFF"), width / 2, 36, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}