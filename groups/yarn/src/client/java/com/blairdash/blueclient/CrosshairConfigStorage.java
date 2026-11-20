package com.blairdash.blueclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Saves/loads all settings to %LOCALAPPDATA%/BlueClient/settings.json.
 * (Despite the name, this covers every setting, not just the crosshair.)
 * If settings.json doesn't exist yet, the old crosshair.json is read instead
 * so existing crosshair settings carry over.
 */
public class CrosshairConfigStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static Path configDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = (localAppData != null)
                ? Paths.get(localAppData)
                : Paths.get(System.getProperty("user.home"), ".config");
        return base.resolve("BlueClient");
    }

    private static Path settingsFile() {
        return configDir().resolve("settings.json");
    }

    private static Path legacyCrosshairFile() {
        return configDir().resolve("crosshair.json");
    }

    static class Data {
        // crosshair
        int color = ModConfig.crosshairColor;
        float size = ModConfig.crosshairSize;
        float thickness = ModConfig.crosshairThickness;
        float gap = ModConfig.crosshairGap;
        boolean enemyEnabled = ModConfig.enemyCrosshairEnabled;
        int enemyColor = ModConfig.enemyCrosshairColor;

        // toggles
        boolean zoomEnabled = ModConfig.zoomEnabled;
        boolean crosshairEnabled = ModConfig.customCrosshairEnabled;
        boolean cpsFpsHudEnabled = ModConfig.cpsFpsHudEnabled;
        boolean coordinatesHudEnabled = ModConfig.coordinatesHudEnabled;
        boolean motionBlurEnabled = ModConfig.motionBlurEnabled;
        boolean selfNametagEnabled = ModConfig.selfNametagEnabled;
        boolean potionHudEnabled = ModConfig.potionHudEnabled;
        boolean armorHudEnabled = ModConfig.armorHudEnabled;
        boolean lowFireEnabled = ModConfig.lowFireEnabled;
        boolean lowShieldEnabled = ModConfig.lowShieldEnabled;
        boolean cheatWatchEnabled = ModConfig.cheatWatchEnabled;
        boolean keystrokesEnabled = ModConfig.keystrokesEnabled;
        boolean fullbrightEnabled = ModConfig.fullbrightEnabled;
        boolean noHurtCamEnabled = ModConfig.noHurtCamEnabled;
        boolean timeChangerEnabled = ModConfig.timeChangerEnabled;
        double timeOfDayValue = ModConfig.timeOfDayValue;
        boolean clearWeatherEnabled = ModConfig.clearWeatherEnabled;
        boolean chatTimestampsEnabled = ModConfig.chatTimestampsEnabled;
        boolean hideScoreboardEnabled = ModConfig.hideScoreboardEnabled;

        // first-person offsets
        float lowFireOffset = ModConfig.lowFireOffset;
        float lowShieldOffset = ModConfig.lowShieldOffset;

        // HUD positions
        int cpsFpsX = ModConfig.cpsFpsX;
        int cpsFpsY = ModConfig.cpsFpsY;
        int potionHudX = ModConfig.potionHudX;
        int potionHudY = ModConfig.potionHudY;
        int coordsX = ModConfig.coordsX;
        int coordsY = ModConfig.coordsY;
        int armorHudX = ModConfig.armorHudX;
        int armorHudY = ModConfig.armorHudY;
        int keystrokesX = ModConfig.keystrokesX;
        int keystrokesY = ModConfig.keystrokesY;

        // essentials batch
        boolean toggleSprint = ModConfig.toggleSprint;
        boolean toggleSneak = ModConfig.toggleSneak;
        boolean freelookEnabled = ModConfig.freelookEnabled;
        boolean hitboxesEnabled = ModConfig.hitboxesEnabled;
        boolean[] infoEnabled = ModConfig.infoEnabled.clone();
        int[] infoX = ModConfig.infoX.clone();
        int[] infoY = ModConfig.infoY.clone();

        // Blue mods (see com.blairdash.blueclient.mods)
        java.util.Map<String, java.util.Map<String, Object>> mods = com.blairdash.blueclient.mods.ModManager.export();
    }

    public static void load() {
        readActiveName();
        applyFile(Files.exists(settingsFile()) ? settingsFile() : legacyCrosshairFile());
    }

    private static void applyFile(Path file) {
        try {
            if (!Files.exists(file)) return;
            Data data = GSON.fromJson(Files.readString(file), Data.class);
            if (data == null) return;

            ModConfig.crosshairColor = data.color;
            ModConfig.crosshairSize = data.size;
            ModConfig.crosshairThickness = data.thickness;
            ModConfig.crosshairGap = data.gap;
            ModConfig.enemyCrosshairEnabled = data.enemyEnabled;
            ModConfig.enemyCrosshairColor = data.enemyColor;

            ModConfig.zoomEnabled = data.zoomEnabled;
            ModConfig.customCrosshairEnabled = data.crosshairEnabled;
            ModConfig.cpsFpsHudEnabled = data.cpsFpsHudEnabled;
            ModConfig.coordinatesHudEnabled = data.coordinatesHudEnabled;
            ModConfig.motionBlurEnabled = data.motionBlurEnabled;
            ModConfig.selfNametagEnabled = data.selfNametagEnabled;
            ModConfig.potionHudEnabled = data.potionHudEnabled;
            ModConfig.armorHudEnabled = data.armorHudEnabled;
            ModConfig.lowFireEnabled = data.lowFireEnabled;
            ModConfig.lowShieldEnabled = data.lowShieldEnabled;
            ModConfig.cheatWatchEnabled = data.cheatWatchEnabled;
            ModConfig.keystrokesEnabled = data.keystrokesEnabled;
            ModConfig.fullbrightEnabled = data.fullbrightEnabled;
            ModConfig.noHurtCamEnabled = data.noHurtCamEnabled;
            ModConfig.timeChangerEnabled = data.timeChangerEnabled;
            ModConfig.timeOfDayValue = data.timeOfDayValue;
            ModConfig.clearWeatherEnabled = data.clearWeatherEnabled;
            ModConfig.chatTimestampsEnabled = data.chatTimestampsEnabled;
            ModConfig.hideScoreboardEnabled = data.hideScoreboardEnabled;

            ModConfig.lowFireOffset = data.lowFireOffset;
            ModConfig.lowShieldOffset = data.lowShieldOffset;

            ModConfig.cpsFpsX = data.cpsFpsX;
            ModConfig.cpsFpsY = data.cpsFpsY;
            ModConfig.potionHudX = data.potionHudX;
            ModConfig.potionHudY = data.potionHudY;
            ModConfig.coordsX = data.coordsX;
            ModConfig.coordsY = data.coordsY;
            ModConfig.armorHudX = data.armorHudX;
            ModConfig.armorHudY = data.armorHudY;
            ModConfig.keystrokesX = data.keystrokesX;
            ModConfig.keystrokesY = data.keystrokesY;

            ModConfig.toggleSprint = data.toggleSprint;
            ModConfig.toggleSneak = data.toggleSneak;
            ModConfig.freelookEnabled = data.freelookEnabled;
            ModConfig.hitboxesEnabled = data.hitboxesEnabled;
            if (data.infoEnabled != null && data.infoEnabled.length == ModConfig.INFO_COUNT) ModConfig.infoEnabled = data.infoEnabled;
            if (data.infoX != null && data.infoX.length == ModConfig.INFO_COUNT) ModConfig.infoX = data.infoX;
            if (data.infoY != null && data.infoY.length == ModConfig.INFO_COUNT) ModConfig.infoY = data.infoY;
            com.blairdash.blueclient.mods.ModManager.apply(data.mods);
            System.out.println("[blueclient] loaded settings from " + file + " (crosshair color=" + Integer.toHexString(ModConfig.crosshairColor) + ", gap=" + ModConfig.crosshairGap + ", size=" + ModConfig.crosshairSize + ")");
        } catch (IOException | RuntimeException e) {
            System.err.println("[clientmod] Could not load settings: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(configDir());
            String json = GSON.toJson(new Data());
            Files.writeString(settingsFile(), json);
            // Keep the active profile in sync, so tweaks are never lost when switching.
            Files.createDirectories(profilesDir());
            Files.writeString(profileFile(activeProfile), json);
        } catch (IOException e) {
            System.err.println("[clientmod] Could not save settings: " + e.getMessage());
        }
    }

    // --- profiles ---------------------------------------------------------

    public static final String DEFAULT_PROFILE = "Default";
    private static String activeProfile = DEFAULT_PROFILE;

    private static Path profilesDir() {
        return configDir().resolve("profiles");
    }

    private static Path profileFile(String name) {
        return profilesDir().resolve(name + ".json");
    }

    private static Path activeProfileFile() {
        return configDir().resolve("active_profile.txt");
    }

    private static String sanitize(String name) {
        String clean = name == null ? "" : name.replaceAll("[^A-Za-z0-9 _-]", "").trim();
        if (clean.length() > 24) clean = clean.substring(0, 24).trim();
        return clean;
    }

    private static void readActiveName() {
        try {
            if (Files.exists(activeProfileFile())) {
                String n = sanitize(Files.readString(activeProfileFile()));
                if (!n.isEmpty()) activeProfile = n;
            }
        } catch (IOException ignored) { }
    }

    private static void writeActiveName() {
        try {
            Files.createDirectories(configDir());
            Files.writeString(activeProfileFile(), activeProfile);
        } catch (IOException ignored) { }
    }

    public static String getActiveProfile() {
        return activeProfile;
    }

    /** All saved profile names, Default first, always including the active one. */
    public static List<String> listProfiles() {
        List<String> names = new ArrayList<>();
        try {
            if (Files.isDirectory(profilesDir())) {
                try (var stream = Files.list(profilesDir())) {
                    stream.map(p -> p.getFileName().toString())
                            .filter(n -> n.endsWith(".json"))
                            .map(n -> n.substring(0, n.length() - 5))
                            .forEach(names::add);
                }
            }
        } catch (IOException ignored) { }
        if (!names.contains(DEFAULT_PROFILE)) names.add(DEFAULT_PROFILE);
        if (!names.contains(activeProfile)) names.add(activeProfile);
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        names.remove(DEFAULT_PROFILE);
        names.add(0, DEFAULT_PROFILE);
        return names;
    }

    /** Loads a profile's settings into the game and makes it the active one. */
    public static void applyProfile(String name) {
        save(); // flush the current profile first
        Path file = profileFile(name);
        if (!Files.exists(file)) return;
        applyFile(file);
        activeProfile = name;
        writeActiveName();
        save();
    }

    /** Saves the current settings as a new profile and switches to it. Returns false if the name is unusable. */
    public static boolean saveProfileAs(String rawName) {
        String name = sanitize(rawName);
        if (name.isEmpty()) return false;
        save(); // flush the current profile first
        activeProfile = name;
        writeActiveName();
        save();
        return true;
    }

    public static void deleteProfile(String name) {
        if (DEFAULT_PROFILE.equals(name)) return;
        try {
            Files.deleteIfExists(profileFile(name));
        } catch (IOException ignored) { }
        if (name.equals(activeProfile)) {
            // Don't flush first: that would just re-create the profile being deleted.
            activeProfile = DEFAULT_PROFILE;
            writeActiveName();
            applyFile(profileFile(DEFAULT_PROFILE));
            save();
        }
    }
}
