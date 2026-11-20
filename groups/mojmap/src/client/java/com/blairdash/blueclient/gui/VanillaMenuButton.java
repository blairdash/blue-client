package com.blairdash.blueclient.gui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/** Adds a "Blue Client menu" button to the vanilla title screen so you can switch back from Vanilla style. */
public final class VanillaMenuButton {
    private VanillaMenuButton() { }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen.getClass() != TitleScreen.class || BlueMenuConfig.blueMenu()) return;
            Screens.getWidgets(screen).add(Button.builder(Component.literal("Blue Client menu"), b -> {
                BlueMenuConfig.setBlueMenu(true);
                client.setScreen(new BlueTitleScreen());
            }).bounds(6, 6, 110, 20).build());
            Screens.getWidgets(screen).add(Button.builder(Component.literal("Menu layout"), b ->
                    client.setScreen(new BlueThemeScreen(screen))).bounds(6, 30, 110, 20).build());
        });
    }
}
