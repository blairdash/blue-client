package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Locale;

/** Set how far Low Fire and Low Shield move things down (larger = lower). */
public class ViewOffsetScreen extends Screen {
    private static final float FIRE_MAX = 0.80f;
    private static final float SHIELD_MAX = 0.80f;
    private static final float FIRE_DEFAULT = 0.45f;
    private static final float SHIELD_DEFAULT = 0.25f;

    private final Screen parent;
    private TextFieldWidget fireField;
    private TextFieldWidget shieldField;

    public ViewOffsetScreen(Screen parent) {
        super(Text.literal("Fire & Shield Offsets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        fireField = field(centerX + 30, 56, ModConfig.lowFireOffset);
        shieldField = field(centerX + 30, 84, ModConfig.lowShieldOffset);

        int buttonsY = 122;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> saveAndClose())
                .dimensions(centerX - 126, buttonsY, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> resetToDefaults())
                .dimensions(centerX - 40, buttonsY, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
                .dimensions(centerX + 46, buttonsY, 80, 20).build());
    }

    private TextFieldWidget field(int x, int y, float value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, 70, 20, Text.literal(""));
        f.setMaxLength(6);
        f.setText(String.format(Locale.ROOT, "%.2f", value));
        addDrawableChild(f);
        return f;
    }

    private static float parse(TextFieldWidget f, float fallback, float max) {
        try {
            float v = Float.parseFloat(f.getText().trim().replace(',', '.'));
            return Math.max(0.0f, Math.min(v, max));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void saveAndClose() {
        ModConfig.lowFireOffset = parse(fireField, ModConfig.lowFireOffset, FIRE_MAX);
        ModConfig.lowShieldOffset = parse(shieldField, ModConfig.lowShieldOffset, SHIELD_MAX);
        CrosshairConfigStorage.save();
        close();
    }

    private void resetToDefaults() {
        ModConfig.lowFireOffset = FIRE_DEFAULT;
        ModConfig.lowShieldOffset = SHIELD_DEFAULT;
        CrosshairConfigStorage.save();
        clearChildren();
        init();
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = width / 2;
        context.drawCenteredTextWithShadow(textRenderer, title, centerX, 20, 0xFFFFFFFF);

        int labelX = centerX - 130;
        context.drawTextWithShadow(textRenderer, "Low Fire offset (0 - 0.80)", labelX, 62, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Low Shield offset (0 - 0.80)", labelX, 90, 0xFFFFFFFF);

        context.drawCenteredTextWithShadow(textRenderer, "Larger = lower. Turn each on from the main menu.",
                centerX, 156, 0xFFAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}