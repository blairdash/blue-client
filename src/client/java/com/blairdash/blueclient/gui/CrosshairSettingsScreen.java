package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class CrosshairSettingsScreen extends Screen {
    private final Screen parent;

    private TextFieldWidget colorField;
    private TextFieldWidget sizeField;
    private TextFieldWidget thicknessField;
    private TextFieldWidget gapField;
    private TextFieldWidget enemyColorField;

    private boolean enemyEnabled;
    private ButtonWidget enemyToggleButton;

    public CrosshairSettingsScreen(Screen parent) {
        super(Text.literal("Custom Crosshair"));
        this.parent = parent;
        this.enemyEnabled = ModConfig.enemyCrosshairEnabled;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int y = 50;
        int fieldWidth = 200;

        colorField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, y, fieldWidth, 20, Text.literal("Color"));
        colorField.setMaxLength(8);
        colorField.setText(String.format("%08X", ModConfig.crosshairColor));
        addDrawableChild(colorField);
        y += 28;

        sizeField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, y, fieldWidth, 20, Text.literal("Size"));
        sizeField.setText(String.valueOf((int) ModConfig.crosshairSize));
        addDrawableChild(sizeField);
        y += 28;

        thicknessField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, y, fieldWidth, 20, Text.literal("Thickness"));
        thicknessField.setText(String.valueOf((int) ModConfig.crosshairThickness));
        addDrawableChild(thicknessField);
        y += 28;

        gapField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, y, fieldWidth, 20, Text.literal("Gap"));
        gapField.setText(String.valueOf((int) ModConfig.crosshairGap));
        addDrawableChild(gapField);
        y += 36;

        enemyToggleButton = ButtonWidget.builder(enemyToggleText(), b -> {
            enemyEnabled = !enemyEnabled;
            b.setMessage(enemyToggleText());
        }).dimensions(centerX - fieldWidth / 2, y, fieldWidth, 20).build();
        addDrawableChild(enemyToggleButton);
        y += 28;

        enemyColorField = new TextFieldWidget(textRenderer, centerX - fieldWidth / 2, y, fieldWidth, 20, Text.literal("Enemy Color"));
        enemyColorField.setMaxLength(8);
        enemyColorField.setText(String.format("%08X", ModConfig.enemyCrosshairColor));
        addDrawableChild(enemyColorField);
        y += 36;

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> saveAndClose())
                .dimensions(centerX - 104, y, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
                .dimensions(centerX + 4, y, 100, 20).build());
    }

    private Text enemyToggleText() {
        return Text.literal("Enemy Crosshair: " + (enemyEnabled ? "ON" : "OFF"));
    }

    private void saveAndClose() {
        try {
            ModConfig.crosshairColor = (int) Long.parseLong(colorField.getText().trim(), 16);
        } catch (NumberFormatException ignored) { }
        try {
            ModConfig.enemyCrosshairColor = (int) Long.parseLong(enemyColorField.getText().trim(), 16);
        } catch (NumberFormatException ignored) { }
        ModConfig.crosshairSize = parseFloatSafe(sizeField.getText(), ModConfig.crosshairSize);
        ModConfig.crosshairThickness = parseFloatSafe(thicknessField.getText(), ModConfig.crosshairThickness);
        ModConfig.crosshairGap = parseFloatSafe(gapField.getText(), ModConfig.crosshairGap);
        ModConfig.enemyCrosshairEnabled = enemyEnabled;
        CrosshairConfigStorage.save();
        close();
    }

    private static float parseFloatSafe(String text, float fallback) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Colors are ARGB hex, e.g. FFFFFFFF"), width / 2, 36, 0xFFAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}