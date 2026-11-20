package com.blairdash.blueclient.gui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Adds a "Blue Client menu" button to the vanilla title screen so you can switch back from Vanilla style. */
public final class VanillaMenuButton {
    private VanillaMenuButton() { }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen.getClass() != TitleScreen.class || BlueMenuConfig.blueMenu()) return;
            Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Blue Client menu"), b -> {
                BlueMenuConfig.setBlueMenu(true);
                client.setScreen(new BlueTitleScreen());
            }).dimensions(6, 6, 110, 20).build());
            Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Menu layout"), b ->
                    client.setScreen(new BlueThemeScreen(screen))).dimensions(6, 30, 110, 20).build());
        });
    }
}
