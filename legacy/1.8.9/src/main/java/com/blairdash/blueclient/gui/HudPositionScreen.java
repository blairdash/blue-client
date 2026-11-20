package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.ConfigStorage;
import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.features.ArmorHud;
import com.blairdash.blueclient.features.CoordinatesHud;
import com.blairdash.blueclient.features.Keystrokes;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

/** Set the X/Y position of the CPS/FPS display, Potion HUD, Coordinates, Armor HUD, and Keystrokes. */
public class HudPositionScreen extends GuiScreen {
    private static final int ID_SAVE = 1;
    private static final int ID_RESET = 2;
    private static final int ID_CANCEL = 3;

    private final GuiScreen parent;

    private GuiTextField cpsX, cpsY, potionX, potionY, coordsX, coordsY, armorX, armorY, keysX, keysY;
    private GuiTextField[] fields;
    private int xColumn, yColumn;
    private int coordsDefaultX, coordsDefaultY, armorDefaultX, armorDefaultY, keysDefaultX, keysDefaultY;

    public HudPositionScreen(GuiScreen parent) {
        this.parent = parent;
    }

    private static int rowY(int row) {
        return 44 + row * 24;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int centerX = this.width / 2;
        xColumn = centerX - 30;
        yColumn = centerX + 40;

        cpsX = field(0, xColumn, rowY(0), ModConfig.cpsFpsX);
        cpsY = field(1, yColumn, rowY(0), ModConfig.cpsFpsY);
        potionX = field(2, xColumn, rowY(1), ModConfig.potionHudX);
        potionY = field(3, yColumn, rowY(1), ModConfig.potionHudY);

        coordsDefaultX = CoordinatesHud.effectiveX(this.mc, this.width);
        coordsDefaultY = CoordinatesHud.effectiveY();
        coordsX = field(4, xColumn, rowY(2), coordsDefaultX);
        coordsY = field(5, yColumn, rowY(2), coordsDefaultY);

        armorDefaultX = ArmorHud.effectiveX(this.width);
        armorDefaultY = ArmorHud.effectiveY(this.height);
        armorX = field(6, xColumn, rowY(3), armorDefaultX);
        armorY = field(7, yColumn, rowY(3), armorDefaultY);

        keysDefaultX = Keystrokes.effectiveX();
        keysDefaultY = Keystrokes.effectiveY(this.height);
        keysX = field(8, xColumn, rowY(4), keysDefaultX);
        keysY = field(9, yColumn, rowY(4), keysDefaultY);

        fields = new GuiTextField[] { cpsX, cpsY, potionX, potionY, coordsX, coordsY, armorX, armorY, keysX, keysY };

        int buttonsY = rowY(4) + 30;
        this.buttonList.add(new GuiButton(ID_SAVE, centerX - 126, buttonsY, 80, 20, "Save"));
        this.buttonList.add(new GuiButton(ID_RESET, centerX - 40, buttonsY, 80, 20, "Reset"));
        this.buttonList.add(new GuiButton(ID_CANCEL, centerX + 46, buttonsY, 80, 20, "Cancel"));
    }

    private GuiTextField field(int id, int x, int y, int value) {
        GuiTextField f = new GuiTextField(id, this.fontRendererObj, x, y, 60, 20);
        f.setMaxStringLength(5);
        f.setText(String.valueOf(value));
        return f;
    }

    private static int parse(GuiTextField f, int fallback, int max) {
        try {
            int v = Integer.parseInt(f.getText().trim());
            return Math.max(0, Math.min(v, max));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void saveAndClose() {
        ModConfig.cpsFpsX = parse(cpsX, ModConfig.cpsFpsX, this.width - 1);
        ModConfig.cpsFpsY = parse(cpsY, ModConfig.cpsFpsY, this.height - 1);
        ModConfig.potionHudX = parse(potionX, ModConfig.potionHudX, this.width - 1);
        ModConfig.potionHudY = parse(potionY, ModConfig.potionHudY, this.height - 1);

        // Coordinates, Armor HUD and Keystrokes: if still on the automatic default and the
        // value wasn't changed, keep it automatic instead of freezing it.
        int cx = parse(coordsX, coordsDefaultX, this.width - 1);
        int cy = parse(coordsY, coordsDefaultY, this.height - 1);
        ModConfig.coordsX = (ModConfig.coordsX < 0 && cx == coordsDefaultX) ? -1 : cx;
        ModConfig.coordsY = (ModConfig.coordsY < 0 && cy == coordsDefaultY) ? -1 : cy;

        int ax = parse(armorX, armorDefaultX, this.width - 1);
        int ay = parse(armorY, armorDefaultY, this.height - 1);
        ModConfig.armorHudX = (ModConfig.armorHudX < 0 && ax == armorDefaultX) ? -1 : ax;
        ModConfig.armorHudY = (ModConfig.armorHudY < 0 && ay == armorDefaultY) ? -1 : ay;

        int kx = parse(keysX, keysDefaultX, this.width - 1);
        int ky = parse(keysY, keysDefaultY, this.height - 1);
        ModConfig.keystrokesX = (ModConfig.keystrokesX < 0 && kx == keysDefaultX) ? -1 : kx;
        ModConfig.keystrokesY = (ModConfig.keystrokesY < 0 && ky == keysDefaultY) ? -1 : ky;

        ConfigStorage.save();
        this.mc.displayGuiScreen(parent);
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
        ConfigStorage.save();
        initGui();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == ID_SAVE) saveAndClose();
        else if (button.id == ID_RESET) resetToDefaults();
        else if (button.id == ID_CANCEL) this.mc.displayGuiScreen(parent);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_TAB) {
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isFocused()) {
                    fields[i].setFocused(false);
                    fields[(i + 1) % fields.length].setFocused(true);
                    return;
                }
            }
            fields[0].setFocused(true);
            return;
        }
        for (GuiTextField f : fields) f.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode); // Esc closes
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
        drawCenteredString(this.fontRendererObj, "HUD Positions", centerX, 12, 0xFFFFFFFF);
        drawCenteredString(this.fontRendererObj, "X", xColumn + 30, 30, 0xFFAAAAAA);
        drawCenteredString(this.fontRendererObj, "Y", yColumn + 30, 30, 0xFFAAAAAA);

        int labelX = centerX - 110;
        drawString(this.fontRendererObj, "CPS / FPS", labelX, rowY(0) + 6, 0xFFFFFFFF);
        drawString(this.fontRendererObj, "Potion HUD", labelX, rowY(1) + 6, 0xFFFFFFFF);
        drawString(this.fontRendererObj, "Coordinates", labelX, rowY(2) + 6, 0xFFFFFFFF);
        drawString(this.fontRendererObj, "Armor HUD", labelX, rowY(3) + 6, 0xFFFFFFFF);
        drawString(this.fontRendererObj, "Keystrokes", labelX, rowY(4) + 6, 0xFFFFFFFF);

        int noteY = rowY(4) + 56;
        drawCenteredString(this.fontRendererObj, "X / Y = pixels from the top-left of the screen.", centerX, noteY, 0xFFAAAAAA);

        for (GuiTextField f : fields) f.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
