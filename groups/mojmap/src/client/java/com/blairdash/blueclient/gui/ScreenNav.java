package com.blairdash.blueclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;

/** Opens screens; these calls moved/changed between Minecraft versions, so they live in one place. */
final class ScreenNav {
    private ScreenNav() { }
    static void open(Screen screen) { Minecraft.getInstance().gui.setScreen(screen); }
    static Screen options(Screen parent) { return new OptionsScreen(parent, Minecraft.getInstance().options); }
}
