package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import com.blairdash.blueclient.ModConfig;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Set how far Low Fire and Low Shield move things down (larger = lower). */
public class ViewOffsetScreen extends Screen {
    private static final float FIRE_MAX = 0.80f;
    private static final float SHIELD_MAX = 0.80f;
    private static final float FIRE_DEFAULT = 0.45f;
    private static final float SHIELD_DEFAULT = 0.25f;

    private final Screen parent;
    private EditBox fireField;
    private EditBox shieldField;

    public ViewOffsetScreen(Screen parent) {
        super(Component.literal("Fire & Shield Offsets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        fireField = field(centerX + 30, 56, ModConfig.lowFireOffset);
        shieldField = field(centerX + 30, 84, ModConfig.lowShieldOffset);

        int buttonsY = 122;
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAndClose())
                .bounds(centerX - 126, buttonsY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> resetToDefaults())
                .bounds(centerX - 40, buttonsY, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(centerX + 46, buttonsY, 80, 20).build());
    }

    private EditBox field(int x, int y, float value) {
        EditBox f = new EditBox(font, x, y, 70, 20, Component.literal(""));
        f.setMaxLength(6);
        f.setValue(String.format(Locale.ROOT, "%.2f", value));
        addRenderableWidget(f);
        return f;
    }

    private static float parse(EditBox f, float fallback, float max) {
        try {
            float v = Float.parseFloat(f.getValue().trim().replace(',', '.'));
            return Math.max(0.0f, Math.min(v, max));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void saveAndClose() {
        ModConfig.lowFireOffset = parse(fireField, ModConfig.lowFireOffset, FIRE_MAX);
        ModConfig.lowShieldOffset = parse(shieldField, ModConfig.lowShieldOffset, SHIELD_MAX);
        CrosshairConfigStorage.save();
        onClose();
    }

    private void resetToDefaults() {
        ModConfig.lowFireOffset = FIRE_DEFAULT;
        ModConfig.lowShieldOffset = SHIELD_DEFAULT;
        CrosshairConfigStorage.save();
        clearWidgets();
        init();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context) {
        int mouseX = context.mouseX(), mouseY = context.mouseY(); float delta = context.partialTicks();
        super.extractRenderState(context);
        int centerX = width / 2;
        context.centeredText(font, title, centerX, 20, 0xFFFFFFFF);

        int labelX = centerX - 130;
        context.text(font, "Low Fire offset (0 - 0.80)", labelX, 62, 0xFFFFFFFF);
        context.text(font, "Low Shield offset (0 - 0.80)", labelX, 90, 0xFFFFFFFF);

        context.centeredText(font, "Larger = lower. Turn each on from the main menu.",
                centerX, 156, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}