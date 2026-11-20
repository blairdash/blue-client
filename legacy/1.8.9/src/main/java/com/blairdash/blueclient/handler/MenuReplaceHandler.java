package com.blairdash.blueclient.handler;

import com.blairdash.blueclient.gui.BlueMenuConfig;
import com.blairdash.blueclient.gui.BlueTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Swaps vanilla's main menu for the Blue Client one, and adds a way back from the vanilla menu. */
public class MenuReplaceHandler {
    private static final int BACK_BUTTON_ID = 9001, LAYOUT_BUTTON_ID = 9002;

    @SubscribeEvent
    public void onOpen(GuiOpenEvent e) {
        if (e.gui != null && e.gui.getClass() == GuiMainMenu.class && BlueMenuConfig.blueMenu()) e.gui = new BlueTitleScreen();
    }

    /** "Blue Client menu" button in the top-left of the vanilla title screen. */
    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onInit(GuiScreenEvent.InitGuiEvent.Post e) {
        if (e.gui != null && e.gui.getClass() == GuiMainMenu.class && !BlueMenuConfig.blueMenu())
        {
            e.buttonList.add(new GuiButton(BACK_BUTTON_ID, 6, 6, 110, 20, "Blue Client menu"));
            e.buttonList.add(new GuiButton(LAYOUT_BUTTON_ID, 6, 30, 110, 20, "Menu layout"));
        }
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Post e) {
        if (e.gui != null && e.gui.getClass() == GuiMainMenu.class && e.button.id == LAYOUT_BUTTON_ID) {
            Minecraft.getMinecraft().displayGuiScreen(new com.blairdash.blueclient.gui.BlueThemeScreen(e.gui));
            return;
        }
        if (e.gui != null && e.gui.getClass() == GuiMainMenu.class && e.button.id == BACK_BUTTON_ID) {
            BlueMenuConfig.setBlueMenu(true);
            Minecraft.getMinecraft().displayGuiScreen(new BlueTitleScreen());
        }
    }
}
