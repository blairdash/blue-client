package com.blairdash.blueclient.gui;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Main-menu settings: style (Blue / Vanilla) and background theme. Stored in config/blueclient-menu.properties. */
public final class BlueMenuConfig {
    private BlueMenuConfig() { }

    public static final String[] THEMES = {"Panorama", "Midnight", "Ocean"};
    private static boolean blueMenu = true;
    private static int theme = 0;
    private static boolean loaded;

    /** Shared by every Minecraft version/instance (same folder as the other Blue Client settings). */
    private static Path file() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = localAppData != null ? Path.of(localAppData) : Path.of(System.getProperty("user.home"), ".config");
        Path dir = base.resolve("BlueClient");
        try { Files.createDirectories(dir); } catch (IOException ignored) { }
        return dir.resolve("menu.properties");
    }

    /** Older builds kept this per game instance; used once as a fallback so nothing is lost. */
    private static Path legacyFile() { return FabricLoader.getInstance().getConfigDir().resolve("blueclient-menu.properties"); }

    private static void load() {
        if (loaded) return;
        loaded = true;
        Path f = file();
        if (!Files.exists(f)) f = legacyFile();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            Properties p = new Properties();
            p.load(r);
            blueMenu = !"vanilla".equals(p.getProperty("style", "blue"));
            theme = Math.max(0, Math.min(THEMES.length - 1, Integer.parseInt(p.getProperty("theme", "0").trim()))); // migrate
        } catch (IOException | NumberFormatException ignored) { }
        if (!f.equals(file())) save(); // migrate the old per-instance file to the shared one
    }

    private static void save() {
        try (Writer w = Files.newBufferedWriter(file())) {
            Properties p = new Properties();
            p.setProperty("style", blueMenu ? "blue" : "vanilla");
            p.setProperty("theme", Integer.toString(theme));
            p.store(w, "Blue Client main menu");
        } catch (IOException ignored) { }
    }

    public static boolean blueMenu() { load(); return blueMenu; }
    public static void setBlueMenu(boolean v) { load(); blueMenu = v; save(); }
    public static int theme() { load(); return theme; }
    public static void setTheme(int t) { load(); theme = t; save(); }
}
