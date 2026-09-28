package com.blairdash.blueclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
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
    }

    public static void load() {
        try {
            Path file = Files.exists(settingsFile()) ? settingsFile() : legacyCrosshairFile();
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
        } catch (IOException | RuntimeException e) {
            System.err.println("[clientmod] Could not load settings: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(configDir());
            Files.writeString(settingsFile(), GSON.toJson(new Data()));
        } catch (IOException e) {
            System.err.println("[clientmod] Could not save settings: " + e.getMessage());
        }
    }
}