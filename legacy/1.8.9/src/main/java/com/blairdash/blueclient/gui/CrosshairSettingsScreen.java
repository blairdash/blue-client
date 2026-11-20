package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.ConfigStorage;
import com.blairdash.blueclient.ModConfig;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

/** Custom crosshair settings (right-click the Crosshair toggle in the main menu). */
public class CrosshairSettingsScreen extends GuiScreen {
    private static final int ID_ENEMY_TOGGLE = 1;
    private static final int ID_SAVE = 2;
    private static final int ID_CANCEL = 3;

    private final GuiScreen parent;

    private GuiTextField colorField, sizeField, thicknessField, gapField, enemyColorField;
    private GuiTextField[] fields;
    private GuiButton enemyToggleButton;
    private boolean enemyEnabled;

    public CrosshairSettingsScreen(GuiScreen parent) {
        this.parent = parent;
        this.enemyEnabled = ModConfig.enemyCrosshairEnabled;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int centerX = this.width / 2;
        int toggleWidth = 200;
        int tx = centerX - toggleWidth / 2;
        int fx = centerX - 30;
        int fw = 140;
        int y = 40;

        colorField = new GuiTextField(0, this.fontRendererObj, fx, y, fw, 20);
        colorField.setMaxStringLength(8);
        colorField.setText(String.format("%08X", ModConfig.crosshairColor));
        y += 24;

        sizeField = new GuiTextField(1, this.fontRendererObj, fx, y, fw, 20);
        sizeField.setText(String.valueOf((int) ModConfig.crosshairSize));
        y += 24;

        thicknessField = new GuiTextField(2, this.fontRendererObj, fx, y, fw, 20);
        thicknessField.setText(String.valueOf((int) ModConfig.crosshairThickness));
        y += 24;

        gapField = new GuiTextField(3, this.fontRendererObj, fx, y, fw, 20);
        gapField.setText(String.valueOf((int) ModConfig.crosshairGap));
        y += 28;

        enemyToggleButton = new GuiButton(ID_ENEMY_TOGGLE, tx, y, toggleWidth, 20, enemyToggleText());
        this.buttonList.add(enemyToggleButton);
        y += 24;

        enemyColorField = new GuiTextField(4, this.fontRendererObj, fx, y, fw, 20);
        enemyColorField.setMaxStringLength(8);
        enemyColorField.setText(String.format("%08X", ModConfig.enemyCrosshairColor));
        y += 28;

        fields = new GuiTextField[] { colorField, sizeField, thicknessField, gapField, enemyColorField };

        this.buttonList.add(new GuiButton(ID_SAVE, centerX - 104, y, 100, 20, "Save"));
        this.buttonList.add(new GuiButton(ID_CANCEL, centerX + 4, y, 100, 20, "Cancel"));
    }

    private String enemyToggleText() {
        return "Enemy Crosshair: " + (enemyEnabled ? "ON" : "OFF");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == ID_ENEMY_TOGGLE) {
            enemyEnabled = !enemyEnabled;
            enemyToggleButton.displayString = enemyToggleText();
        } else if (button.id == ID_SAVE) {
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
            ConfigStorage.save();
            this.mc.displayGuiScreen(parent);
        } else if (button.id == ID_CANCEL) {
            this.mc.displayGuiScreen(parent);
        }
    }

    private static float parseFloatSafe(String text, float fallback) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for (GuiTextField f : fields) f.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (GuiTextField f : fields) f.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        for (GuiTextField f : fields) f.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int centerX = this.width / 2;
        drawCenteredString(this.fontRendererObj, "Custom Crosshair  (colors are ARGB hex, e.g. FFFFFFFF)", centerX, 14, 0xFFFFFFFF);

        int labelX = centerX - 130;
        drawString(this.fontRendererObj, "Color", labelX, 46, 0xFFAAAAAA);
        drawString(this.fontRendererObj, "Size", labelX, 70, 0xFFAAAAAA);
        drawString(this.fontRendererObj, "Thickness", labelX, 94, 0xFFAAAAAA);
        drawString(this.fontRendererObj, "Gap", labelX, 118, 0xFFAAAAAA);
        drawString(this.fontRendererObj, "Enemy color", labelX, 170, 0xFFAAAAAA);

        for (GuiTextField f : fields) f.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
