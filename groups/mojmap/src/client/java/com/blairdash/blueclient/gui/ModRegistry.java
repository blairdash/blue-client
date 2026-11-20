package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** The list of mods shown as cards in the menu. */
final class ModRegistry {
    private ModRegistry() { }

    static final String HUD = "HUD";
    static final String MECHANIC = "MECHANIC";
    static final String VISUAL = "VISUAL";

    static List<ModEntry> create() {
        List<ModEntry> list = new ArrayList<>();

        list.add(new ModEntry("Zoom", MECHANIC, new ItemStack(Items.SPYGLASS),
                () -> ModConfig.zoomEnabled, v -> ModConfig.zoomEnabled = v,
                parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("zoom_opts"))));

        list.add(new ModEntry("Crosshair", VISUAL, new ItemStack(Items.ENDER_EYE),
                () -> ModConfig.customCrosshairEnabled, v -> ModConfig.customCrosshairEnabled = v,
                parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("crosshair_opts"))));

        list.add(new ModEntry("CPS / FPS", HUD, new ItemStack(Items.CLOCK),
                () -> ModConfig.cpsFpsHudEnabled, v -> ModConfig.cpsFpsHudEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("hudstyle_opts"))));

        list.add(new ModEntry("Coordinates", HUD, new ItemStack(Items.COMPASS),
                () -> ModConfig.coordinatesHudEnabled, v -> ModConfig.coordinatesHudEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("hudstyle_opts"))));

        list.add(new ModEntry("Potion HUD", HUD, new ItemStack(Items.POTION),
                () -> ModConfig.potionHudEnabled, v -> ModConfig.potionHudEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("hudstyle_opts"))));

        list.add(new ModEntry("Armor HUD", HUD, new ItemStack(Items.DIAMOND_CHESTPLATE),
                () -> ModConfig.armorHudEnabled, v -> ModConfig.armorHudEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("armor_opts"))));

        list.add(new ModEntry("Keystrokes", HUD, new ItemStack(Items.STONE_BUTTON),
                () -> ModConfig.keystrokesEnabled, v -> ModConfig.keystrokesEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("keystrokes_opts"))));

        String[] infoNames = { "Ping", "Memory", "Clock", "Direction", "Combo Counter", "Reach Display" };
        net.minecraft.world.item.Item[] infoIcons = { Items.REPEATER, Items.REDSTONE, Items.CLOCK, Items.RECOVERY_COMPASS,
                Items.IRON_SWORD, Items.SPECTRAL_ARROW };
        for (int i = 0; i < infoNames.length; i++) {
            final int id = i;
            list.add(new ModEntry(infoNames[i], i >= 4 ? "PVP" : HUD, new ItemStack(infoIcons[i]),
                    () -> ModConfig.infoEnabled[id], v -> ModConfig.infoEnabled[id] = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("hudstyle_opts"))));
        }

        list.add(new ModEntry("Toggle Sprint", MECHANIC, new ItemStack(Items.LEATHER_BOOTS),
                () -> ModConfig.toggleSprint, v -> ModConfig.toggleSprint = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("move_opts"))));
        list.add(new ModEntry("Toggle Sneak", MECHANIC, new ItemStack(Items.CHAINMAIL_LEGGINGS),
                () -> ModConfig.toggleSneak, v -> ModConfig.toggleSneak = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("move_opts"))));
        list.add(new ModEntry("Toggle Status", HUD, new ItemStack(Items.OAK_SIGN),
                () -> ModConfig.infoEnabled[6], v -> ModConfig.infoEnabled[6] = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("hudstyle_opts"))));
        list.add(new ModEntry("Freelook (hold V)", MECHANIC, new ItemStack(Items.ENDER_EYE),
                () -> ModConfig.freelookEnabled, v -> ModConfig.freelookEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("freelook_opts"))));
        list.add(new ModEntry("Hitboxes", VISUAL, new ItemStack(Items.BARRIER),
                () -> ModConfig.hitboxesEnabled, v -> ModConfig.hitboxesEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("hitbox_opts"))));

        list.add(new ModEntry("Motion Blur", VISUAL, new ItemStack(Items.ENDER_PEARL),
                () -> ModConfig.motionBlurEnabled, v -> ModConfig.motionBlurEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("motionblur_opts"))));

        list.add(new ModEntry("Fullbright", VISUAL, new ItemStack(Items.GLOWSTONE),
                () -> ModConfig.fullbrightEnabled, v -> ModConfig.fullbrightEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("fullbright_opts"))));

        list.add(new ModEntry("No Hurt Cam", VISUAL, new ItemStack(Items.IRON_CHESTPLATE),
                () -> ModConfig.noHurtCamEnabled, v -> ModConfig.noHurtCamEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("hurtcam_opts"))));

                list.add(new ModEntry("Time Changer", VISUAL, new ItemStack(Items.CLOCK),
                () -> ModConfig.timeChangerEnabled, v -> ModConfig.timeChangerEnabled = v,
                parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("time_opts"))));

        list.add(new ModEntry("Clear Weather", VISUAL, new ItemStack(Items.SNOWBALL),
                () -> ModConfig.clearWeatherEnabled, v -> ModConfig.clearWeatherEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("weather_opts"))));

        list.add(new ModEntry("Chat Timestamps", MECHANIC, new ItemStack(Items.WRITABLE_BOOK),
                () -> ModConfig.chatTimestampsEnabled, v -> ModConfig.chatTimestampsEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("chat_opts"))));

        list.add(new ModEntry("Hide Scoreboard", HUD, new ItemStack(Items.PAINTING),
                () -> ModConfig.hideScoreboardEnabled, v -> ModConfig.hideScoreboardEnabled = v, parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("scoreboard_opts"))));

        list.add(new ModEntry("Self Nametag", VISUAL, new ItemStack(Items.NAME_TAG),
                () -> ModConfig.selfNametagEnabled, v -> ModConfig.selfNametagEnabled = v, null));

        list.add(new ModEntry("Low Fire", VISUAL, new ItemStack(Items.CAMPFIRE),
                () -> ModConfig.lowFireEnabled, v -> ModConfig.lowFireEnabled = v,
                parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("lowfire_opts"))));

        list.add(new ModEntry("Low Shield", VISUAL, new ItemStack(Items.SHIELD),
                () -> ModConfig.lowShieldEnabled, v -> ModConfig.lowShieldEnabled = v,
                parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.ModManager.get("lowshield_opts"))));

        list.add(new ModEntry("Cheat Watch", MECHANIC, new ItemStack(Items.OBSERVER),
                () -> ModConfig.cheatWatchEnabled, v -> ModConfig.cheatWatchEnabled = v, null));


        for (com.blairdash.blueclient.mods.BlueMod m : com.blairdash.blueclient.mods.ModManager.all()) {
            if (com.blairdash.blueclient.mods.LegacyOptions.CAT.equals(m.category)) continue;
            boolean hasOptions = m.isHud() || m.settings.stream().anyMatch(s -> !s.hidden);
            list.add(new ModEntry(m.name, m.category, new ItemStack(m.icon),
                    () -> m.enabled, v -> m.enabled = v,
                    "particle_changer".equals(m.id)
                            ? parent -> new GenericOptionsScreen(parent, com.blairdash.blueclient.mods.LegacyOptions.holder("particle_opts"))
                            : hasOptions ? parent -> new GenericOptionsScreen(parent, m) : null));
        }

        return list;
    }

    /** Category pills: ALL, the built-in ones, then one per extension tab. */
    static String[] categories() {
        List<String> out = new ArrayList<>(List.of("ALL", HUD, MECHANIC, VISUAL));
        for (String extra : new String[] { "PVP", "UTILITY", "BLUE", "COSMETICS" }) {
            for (com.blairdash.blueclient.mods.BlueMod m : com.blairdash.blueclient.mods.ModManager.all()) {
                if (m.category.equals(extra)) { out.add(extra); break; }
            }
        }
        return out.toArray(new String[0]);
    }
}
