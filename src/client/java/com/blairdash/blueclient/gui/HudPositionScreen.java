package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.features.ArmorHud;
import com.blairdash.blueclient.features.CoordinatesHud;
import com.blairdash.blueclient.features.Keystrokes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Set the X/Y position of the CPS/FPS display, Potion HUD, Coordinates, Armor HUD, and Keystrokes. */
public class HudPositionScreen extends Screen {
    private final Screen parent;

    private TextFieldWidget cpsX, cpsY, potionX, potionY, coordsX, coordsY, armorX, armorY, keysX, keysY;
    private int xColumn, yColumn;
    private int coordsDefaultX, coordsDefaultY, armorDefaultX, armorDefaultY, keysDefaultX, keysDefaultY;

    public HudPositionScreen(Screen parent) {
        super(Text.literal("HUD Positions"));
        this.parent = parent;
    }

    private static int rowY(int row) {
        return 54 + row * 24;
    }

    @Override
    protected void init() {
        MinecraftClient client = MinecraftClient.getInstance();
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
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), b -> saveAndClose())
                .dimensions(centerX - 126, buttonsY, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> resetToDefaults())
                .dimensions(centerX - 40, buttonsY, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close())
                .dimensions(centerX + 46, buttonsY, 80, 20).build());
    }

    private TextFieldWidget field(int x, int y, int value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, 60, 20, Text.literal(""));
        f.setMaxLength(5);
        f.setText(String.valueOf(value));
        addDrawableChild(f);
        return f;
    }

    private static int parse(TextFieldWidget f, int fallback, int max) {
        try {
            int v = Integer.parseInt(f.getText().trim());
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
        close();
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
        context.drawCenteredTextWithShadow(textRenderer, title, centerX, 14, 0xFFFFFFFF);

        context.drawCenteredTextWithShadow(textRenderer, "X", xColumn + 30, 40, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, "Y", yColumn + 30, 40, 0xFFAAAAAA);

        int labelX = centerX - 110;
        context.drawTextWithShadow(textRenderer, "CPS / FPS", labelX, rowY(0) + 6, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Potion HUD", labelX, rowY(1) + 6, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Coordinates", labelX, rowY(2) + 6, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Armor HUD", labelX, rowY(3) + 6, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Keystrokes", labelX, rowY(4) + 6, 0xFFFFFFFF);

        int noteY = rowY(4) + 58;
        context.drawCenteredTextWithShadow(textRenderer, "X / Y = pixels from the top-left of the screen.", centerX, noteY, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, "Armor HUD / Keystrokes: top-left of the display.", centerX, noteY + 11, 0xFFAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}