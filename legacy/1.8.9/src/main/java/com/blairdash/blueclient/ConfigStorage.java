package com.blairdash.blueclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Saves/loads all settings to %LOCALAPPDATA%/BlueClient/settings.json -- the same
 * file and field names the 1.21.x/26.x builds use, so settings carry across versions.
 */
public class ConfigStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static Path configDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = (localAppData != null)
                ? Paths.get(localAppData)
                : Paths.get(System.getProperty("user.home"), ".config");
        return base.resolve("BlueClient");
    }

    private static Path settingsFile() {
        return configDir().resolve("settings.json");
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
        boolean fullbrightEnabled = ModConfig.fullbrightEnabled;
        boolean noHurtCamEnabled = ModConfig.noHurtCamEnabled;
        boolean timeChangerEnabled = ModConfig.timeChangerEnabled;
        double timeOfDayValue = ModConfig.timeOfDayValue;
        boolean clearWeatherEnabled = ModConfig.clearWeatherEnabled;
        boolean chatTimestampsEnabled = ModConfig.chatTimestampsEnabled;
        boolean hideScoreboardEnabled = ModConfig.hideScoreboardEnabled;
        boolean cheatWatchEnabled = ModConfig.cheatWatchEnabled;
        boolean keystrokesEnabled = ModConfig.keystrokesEnabled;

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
        boolean moveTapToggle = ModConfig.moveTapToggle;
        boolean moveStartOn = ModConfig.moveStartOn;
        boolean moveFlyBoost = ModConfig.moveFlyBoost;
        int moveFlyAmount = ModConfig.moveFlyAmount;
        boolean moveSneakContainer = ModConfig.moveSneakContainer;
        boolean moveDetail = ModConfig.moveDetail;
        boolean moveAlwaysSprint = ModConfig.moveAlwaysSprint;
        boolean freelookEnabled = ModConfig.freelookEnabled;
        boolean hitboxesEnabled = ModConfig.hitboxesEnabled;
        boolean[] infoEnabled = ModConfig.infoEnabled.clone();
        int[] infoX = ModConfig.infoX.clone();
        int[] infoY = ModConfig.infoY.clone();

        // extra mods
        java.util.Map<String, Boolean> extraEnabled = new java.util.HashMap<String, Boolean>(ModConfig.extraEnabled);
        java.util.Map<String, Integer> extraMode = new java.util.HashMap<String, Integer>(ModConfig.extraMode);
        java.util.Map<String, Integer> extraX = new java.util.HashMap<String, Integer>(ModConfig.extraX);
        java.util.Map<String, Integer> extraY = new java.util.HashMap<String, Integer>(ModConfig.extraY);
    }

    public static void load() {
        readActiveName();
        applyFile(settingsFile());
    }

    private static void applyFile(Path file) {
        try {
            if (!Files.exists(file)) return;
            Data data = GSON.fromJson(new String(Files.readAllBytes(file), UTF8), Data.class);
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
            ModConfig.fullbrightEnabled = data.fullbrightEnabled;
            ModConfig.noHurtCamEnabled = data.noHurtCamEnabled;
            ModConfig.timeChangerEnabled = data.timeChangerEnabled;
            ModConfig.timeOfDayValue = data.timeOfDayValue;
            ModConfig.clearWeatherEnabled = data.clearWeatherEnabled;
            ModConfig.chatTimestampsEnabled = data.chatTimestampsEnabled;
            ModConfig.hideScoreboardEnabled = data.hideScoreboardEnabled;
            ModConfig.cheatWatchEnabled = data.cheatWatchEnabled;
            ModConfig.keystrokesEnabled = data.keystrokesEnabled;

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
            ModConfig.moveTapToggle = data.moveTapToggle;
            ModConfig.moveStartOn = data.moveStartOn;
            ModConfig.moveFlyBoost = data.moveFlyBoost;
            ModConfig.moveFlyAmount = Math.max(2, Math.min(8, data.moveFlyAmount));
            ModConfig.moveSneakContainer = data.moveSneakContainer;
            ModConfig.moveDetail = data.moveDetail;
            ModConfig.moveAlwaysSprint = data.moveAlwaysSprint;
            ModConfig.freelookEnabled = data.freelookEnabled;
            ModConfig.hitboxesEnabled = data.hitboxesEnabled;
            if (data.infoEnabled != null && data.infoEnabled.length == ModConfig.INFO_COUNT) ModConfig.infoEnabled = data.infoEnabled;
            if (data.infoX != null && data.infoX.length == ModConfig.INFO_COUNT) ModConfig.infoX = data.infoX;
            if (data.infoY != null && data.infoY.length == ModConfig.INFO_COUNT) ModConfig.infoY = data.infoY;
            if (data.extraEnabled != null) ModConfig.extraEnabled = data.extraEnabled;
            if (data.extraMode != null) ModConfig.extraMode = data.extraMode;
            if (data.extraX != null) ModConfig.extraX = data.extraX;
            if (data.extraY != null) ModConfig.extraY = data.extraY;
        } catch (IOException e) {
            BlueClientMod.LOGGER.warn("Could not load settings: " + e.getMessage());
        } catch (RuntimeException e) {
            BlueClientMod.LOGGER.warn("Could not parse settings: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(configDir());
            byte[] json = GSON.toJson(new Data()).getBytes(UTF8);
            Files.write(settingsFile(), json);
            // Keep the active profile in sync so tweaks are never lost when switching.
            Files.createDirectories(profilesDir());
            Files.write(profileFile(activeProfile), json);
        } catch (IOException e) {
            BlueClientMod.LOGGER.warn("Could not save settings: " + e.getMessage());
        }
    }

    // --- profiles ---------------------------------------------------------

    public static final String DEFAULT_PROFILE = "Default";
    private static String activeProfile = DEFAULT_PROFILE;

    private static Path profilesDir() { return configDir().resolve("profiles"); }
    private static Path profileFile(String name) { return profilesDir().resolve(name + ".json"); }
    private static Path activeProfileFile() { return configDir().resolve("active_profile.txt"); }

    private static String sanitize(String name) {
        String clean = name == null ? "" : name.replaceAll("[^A-Za-z0-9 _-]", "").trim();
        if (clean.length() > 24) clean = clean.substring(0, 24).trim();
        return clean;
    }

    private static void readActiveName() {
        try {
            if (Files.exists(activeProfileFile())) {
                String n = sanitize(new String(Files.readAllBytes(activeProfileFile()), UTF8));
                if (!n.isEmpty()) activeProfile = n;
            }
        } catch (IOException ignored) { }
    }

    private static void writeActiveName() {
        try {
            Files.createDirectories(configDir());
            Files.write(activeProfileFile(), activeProfile.getBytes(UTF8));
        } catch (IOException ignored) { }
    }

    public static String getActiveProfile() { return activeProfile; }

    /** All saved profile names, Default first, always including the active one. */
    public static List<String> listProfiles() {
        List<String> names = new ArrayList<String>();
        java.io.File[] files = profilesDir().toFile().listFiles();
        if (files != null) {
            for (java.io.File f : files) {
                String n = f.getName();
                if (n.endsWith(".json")) names.add(n.substring(0, n.length() - 5));
            }
        }
        if (!names.contains(DEFAULT_PROFILE)) names.add(DEFAULT_PROFILE);
        if (!names.contains(activeProfile)) names.add(activeProfile);
        Collections.sort(names, new Comparator<String>() {
            public int compare(String a, String b) { return a.compareToIgnoreCase(b); }
        });
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

    /** Saves the current settings as a new profile and switches to it. False if the name is unusable. */
    public static boolean saveProfileAs(String rawName) {
        String name = sanitize(rawName);
        if (name.isEmpty()) return false;
        save();
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
            // Don't flush first: that would re-create the profile being deleted.
            activeProfile = DEFAULT_PROFILE;
            writeActiveName();
            applyFile(profileFile(DEFAULT_PROFILE));
            save();
        }
    }
}
