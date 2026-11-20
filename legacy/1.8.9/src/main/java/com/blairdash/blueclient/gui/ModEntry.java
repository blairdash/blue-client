package com.blairdash.blueclient.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** One mod card in the menu: its name, category, icon, enabled state, and optional options screen. */
final class ModEntry {
    final String name;
    final String category;
    final ItemStack icon;
    final Supplier<Boolean> enabled;
    final Consumer<Boolean> setEnabled;
    /** Builds the options screen (given the screen to return to), or null if the mod has no options. */
    final Function<GuiScreen, GuiScreen> options;

    ModEntry(String name, String category, ItemStack icon, Supplier<Boolean> enabled, Consumer<Boolean> setEnabled,
             Function<GuiScreen, GuiScreen> options) {
        this.name = name;
        this.category = category;
        this.icon = icon;
        this.enabled = enabled;
        this.setEnabled = setEnabled;
        this.options = options;
    }
}
