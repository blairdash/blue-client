package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.ConfigStorage;
import com.blairdash.blueclient.ModConfig;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

/** Set how far Low Fire moves the flames down (larger = lower). */
public class ViewOffsetScreen extends GuiScreen {
    private static final float FIRE_MAX = 0.80f;
    private static final float FIRE_DEFAULT = 0.45f;

    private static final int ID_SAVE = 1;
    private static final int ID_RESET = 2;
    private static final int ID_CANCEL = 3;

    private final GuiScreen parent;
    private GuiTextField fireField;

    public ViewOffsetScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int centerX = this.width / 2;

        fireField = field(0, centerX + 30, 56, ModConfig.lowFireOffset);

        int buttonsY = 94;
        this.buttonList.add(new GuiButton(ID_SAVE, centerX - 126, buttonsY, 80, 20, "Save"));
        this.buttonList.add(new GuiButton(ID_RESET, centerX - 40, buttonsY, 80, 20, "Reset"));
        this.buttonList.add(new GuiButton(ID_CANCEL, centerX + 46, buttonsY, 80, 20, "Cancel"));
    }

    private GuiTextField field(int id, int x, int y, float value) {
        GuiTextField f = new GuiTextField(id, this.fontRendererObj, x, y, 70, 20);
        f.setMaxStringLength(6);
        f.setText(String.format(Locale.ROOT, "%.2f", value));
        return f;
    }

    private static float parse(GuiTextField f, float fallback, float max) {
        try {
            float v = Float.parseFloat(f.getText().trim().replace(',', '.'));
            return Math.max(0.0f, Math.min(v, max));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == ID_SAVE) {
            ModConfig.lowFireOffset = parse(fireField, ModConfig.lowFireOffset, FIRE_MAX);
            ConfigStorage.save();
            this.mc.displayGuiScreen(parent);
        } else if (button.id == ID_RESET) {
            ModConfig.lowFireOffset = FIRE_DEFAULT;
            ConfigStorage.save();
            initGui();
        } else if (button.id == ID_CANCEL) {
            this.mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        fireField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        fireField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        fireField.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int centerX = this.width / 2;
        drawCenteredString(this.fontRendererObj, "Fire Offset", centerX, 20, 0xFFFFFFFF);

        int labelX = centerX - 130;
        drawString(this.fontRendererObj, "Low Fire offset (0 - 0.80)", labelX, 62, 0xFFFFFFFF);

        drawCenteredString(this.fontRendererObj, "Larger = lower. Turn Low Fire on from the main menu.", centerX, 128, 0xFFAAAAAA);

        fireField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
