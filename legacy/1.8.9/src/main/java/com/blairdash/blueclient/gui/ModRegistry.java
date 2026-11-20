package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The list of mods shown as cards in the menu. */
final class ModRegistry {
    private ModRegistry() { }

    static final String HUD = "HUD";
    static final String MECHANIC = "MECHANIC";
    static final String VISUAL = "VISUAL";

    static List<ModEntry> create() {
        List<ModEntry> list = new ArrayList<ModEntry>();

        list.add(new ModEntry("Zoom", MECHANIC, new ItemStack(Items.ender_eye),
                () -> ModConfig.zoomEnabled, v -> ModConfig.zoomEnabled = v,
                parent -> new SliderOptionsScreen(parent, "Zoom",
                        new SliderControl("Zoom FOV (hold C, scroll to adjust)", 4, 60, 0,
                                () -> ModConfig.zoomFov, v -> ModConfig.zoomFov = v))));

        list.add(new ModEntry("Crosshair", VISUAL, new ItemStack(Items.arrow),
                () -> ModConfig.customCrosshairEnabled, v -> ModConfig.customCrosshairEnabled = v,
                CrosshairSettingsScreen::new));

        list.add(new ModEntry("CPS / FPS", HUD, new ItemStack(Items.clock),
                () -> ModConfig.cpsFpsHudEnabled, v -> ModConfig.cpsFpsHudEnabled = v, HudPositionScreen::new));

        list.add(new ModEntry("Coordinates", HUD, new ItemStack(Items.compass),
                () -> ModConfig.coordinatesHudEnabled, v -> ModConfig.coordinatesHudEnabled = v, HudPositionScreen::new));

        list.add(new ModEntry("Potion HUD", HUD, new ItemStack(Items.potionitem),
                () -> ModConfig.potionHudEnabled, v -> ModConfig.potionHudEnabled = v, HudPositionScreen::new));

        list.add(new ModEntry("Armor HUD", HUD, new ItemStack(Items.diamond_chestplate),
                () -> ModConfig.armorHudEnabled, v -> ModConfig.armorHudEnabled = v, HudPositionScreen::new));

        list.add(new ModEntry("Keystrokes", HUD, new ItemStack(Item.getItemFromBlock(Blocks.stone_button)),
                () -> ModConfig.keystrokesEnabled, v -> ModConfig.keystrokesEnabled = v, HudPositionScreen::new));

        String[] infoNames = { "Ping", "Memory", "Clock", "Direction", "Combo Counter", "Reach Display" };
        Item[] infoIcons = { Items.repeater, Items.redstone, Items.clock, Items.compass, Items.iron_sword, Items.arrow };
        for (int i = 0; i < infoNames.length; i++) {
            final int id = i;
            list.add(new ModEntry(infoNames[i], i >= 4 ? "PVP" : HUD, new ItemStack(infoIcons[i]),
                    () -> ModConfig.infoEnabled[id], v -> ModConfig.infoEnabled[id] = v, HudLayoutScreen::new));
        }

        list.add(new ModEntry("Toggle Sprint", MECHANIC, new ItemStack(Items.leather_boots),
                () -> ModConfig.toggleSprint, v -> ModConfig.toggleSprint = v, null));
        list.add(new ModEntry("Toggle Sneak", MECHANIC, new ItemStack(Items.chainmail_leggings),
                () -> ModConfig.toggleSneak, v -> ModConfig.toggleSneak = v, null));
        list.add(new ModEntry("Toggle Status", HUD, new ItemStack(Items.sign),
                () -> ModConfig.infoEnabled[6], v -> ModConfig.infoEnabled[6] = v, HudLayoutScreen::new));
        list.add(new ModEntry("Freelook (hold V)", MECHANIC, new ItemStack(Items.ender_eye),
                () -> ModConfig.freelookEnabled, v -> ModConfig.freelookEnabled = v, null));
        list.add(new ModEntry("Hitboxes", VISUAL, new ItemStack(Item.getItemFromBlock(Blocks.barrier)),
                () -> ModConfig.hitboxesEnabled, v -> ModConfig.hitboxesEnabled = v, null));

        list.add(new ModEntry("Motion Blur", VISUAL, new ItemStack(Items.ender_pearl),
                () -> ModConfig.motionBlurEnabled, v -> ModConfig.motionBlurEnabled = v, null));

        list.add(new ModEntry("Fullbright", VISUAL, new ItemStack(Items.glowstone_dust),
                () -> ModConfig.fullbrightEnabled, v -> ModConfig.fullbrightEnabled = v, null));

        list.add(new ModEntry("No Hurt Cam", VISUAL, new ItemStack(Items.iron_chestplate),
                () -> ModConfig.noHurtCamEnabled, v -> ModConfig.noHurtCamEnabled = v, null));

        list.add(new ModEntry("Time Changer", VISUAL, new ItemStack(Items.clock),
                () -> ModConfig.timeChangerEnabled, v -> ModConfig.timeChangerEnabled = v,
                parent -> new SliderOptionsScreen(parent, "Time Changer",
                        new SliderControl("Time of day (0 sunrise, 6000 noon, 12000 sunset, 18000 midnight)", 0, 24000, 0,
                                () -> ModConfig.timeOfDayValue, v -> ModConfig.timeOfDayValue = v))));

        list.add(new ModEntry("Clear Weather", VISUAL, new ItemStack(Items.snowball),
                () -> ModConfig.clearWeatherEnabled, v -> ModConfig.clearWeatherEnabled = v, null));

        list.add(new ModEntry("Chat Timestamps", MECHANIC, new ItemStack(Items.writable_book),
                () -> ModConfig.chatTimestampsEnabled, v -> ModConfig.chatTimestampsEnabled = v, null));

        list.add(new ModEntry("Hide Scoreboard", HUD, new ItemStack(Items.painting),
                () -> ModConfig.hideScoreboardEnabled, v -> ModConfig.hideScoreboardEnabled = v, null));

        list.add(new ModEntry("Self Nametag", VISUAL, new ItemStack(Items.name_tag),
                () -> ModConfig.selfNametagEnabled, v -> ModConfig.selfNametagEnabled = v, null));

        list.add(new ModEntry("Low Fire", VISUAL, new ItemStack(Items.fire_charge),
                () -> ModConfig.lowFireEnabled, v -> ModConfig.lowFireEnabled = v,
                parent -> new SliderOptionsScreen(parent, "Low Fire",
                        new SliderControl("Fire offset (larger = lower)", 0.0, 0.8, 2,
                                () -> (double) ModConfig.lowFireOffset, v -> ModConfig.lowFireOffset = v.floatValue()))));

        list.add(new ModEntry("Cheat Watch", MECHANIC, new ItemStack(Items.spider_eye),
                () -> ModConfig.cheatWatchEnabled, v -> ModConfig.cheatWatchEnabled = v, null));

        for (final com.blairdash.blueclient.features.ExtraMods.Mod m : com.blairdash.blueclient.features.ExtraMods.ALL) {
            java.util.function.Function<net.minecraft.client.gui.GuiScreen, net.minecraft.client.gui.GuiScreen> opts = null;
            if (m.modeLabel != null) {
                opts = parent -> {
                    SliderControl[] cs = new SliderControl[1 + m.moreLabels.length];
                    cs[0] = new SliderControl(m.modeLabel, 0, m.modeMax, 0, () -> (double) m.mode(), v -> m.setMode((int) Math.round(v)));
                    for (int i = 1; i < cs.length; i++) {
                        final int k = i;
                        cs[i] = new SliderControl(m.moreLabels[i - 1], 0, m.moreMax[i - 1], 0, () -> (double) m.more(k), v -> m.setMore(k, (int) Math.round(v)));
                    }
                    return new SliderOptionsScreen(parent, m.name, cs);
                };
            } else if (m.hasPosition()) {
                opts = HudLayoutScreen::new;
            }
            list.add(new ModEntry(m.name, m.category, m.icon, m::enabled, m::setEnabled, opts));
        }

        return list;
    }
}
