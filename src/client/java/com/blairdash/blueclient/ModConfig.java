package com.blairdash.blueclient;

/** In-memory config. Everything here is saved to %LOCALAPPDATA%/BlueClient/settings.json. */
public class ModConfig {
    public static boolean zoomEnabled = true;
    public static double zoomFov = 15.0; // degrees while zoomed (scroll wheel changes it in-game)

    public static boolean customCrosshairEnabled = true;
    public static int crosshairColor = 0xFFFFFFFF; // ARGB
    public static float crosshairSize = 8.0f;
    public static float crosshairThickness = 2.0f;
    public static float crosshairGap = 0.0f;
    public static boolean enemyCrosshairEnabled = false;
    public static int enemyCrosshairColor = 0xFFFF5555;

    public static boolean cpsFpsHudEnabled = true;
    public static boolean coordinatesHudEnabled = true;

    public static boolean motionBlurEnabled = false;
    public static float motionBlurStrength = 0.5f; // 0..1

    public static boolean selfNametagEnabled = true;

    public static boolean potionHudEnabled = true;
    public static boolean armorHudEnabled = true;

    // Leftover from the dropped Cheat Watch feature; unused. Safe to delete
    // once you're sure no file references it.
    public static boolean cheatWatchEnabled = false;

    public static boolean lowFireEnabled = false;
    public static float lowFireOffset = 0.45f;   // range 0.00 - 0.80
    public static boolean lowShieldEnabled = false;
    public static float lowShieldOffset = 0.25f; // range 0.00 - 0.80

    // HUD positions, in scaled GUI pixels from the top-left of the screen.
    public static int cpsFpsX = 6;
    public static int cpsFpsY = 6;
    public static int potionHudX = 6;
    public static int potionHudY = 34;
    // -1 = default (top-right corner, right-aligned). Any value >= 0 is an exact position.
    public static int coordsX = -1;
    public static int coordsY = -1;
    // -1 = default (bottom-right corner). Any value >= 0 is an exact position.
    public static int armorHudX = -1;
    public static int armorHudY = -1;
    public static boolean keystrokesEnabled = true;
    // -1 = default (left edge, vertically centered). Any value >= 0 is an exact position.
    public static int keystrokesX = -1;
    public static int keystrokesY = -1;
}