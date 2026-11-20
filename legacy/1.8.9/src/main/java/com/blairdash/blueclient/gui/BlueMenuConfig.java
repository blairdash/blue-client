package com.blairdash.blueclient.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Main-menu settings (style Blue/Vanilla + theme), stored in the shared %LOCALAPPDATA%/BlueClient/menu.properties. */
public final class BlueMenuConfig {
    private BlueMenuConfig() { }

    public static final String[] THEMES = { "Panorama", "Midnight", "Ocean" };
    private static boolean blueMenu = true;
    private static int theme = 0;
    private static boolean loaded;

    private static File file() {
        String base = System.getenv("LOCALAPPDATA");
        File dir = new File(base != null ? new File(base) : new File(System.getProperty("user.home"), ".config"), "BlueClient");
        dir.mkdirs();
        return new File(dir, "menu.properties");
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        File f = file();
        if (!f.exists()) return;
        try {
            FileInputStream in = new FileInputStream(f);
            Properties p = new Properties();
            p.load(in);
            in.close();
            blueMenu = !"vanilla".equals(p.getProperty("style", "blue"));
            theme = Math.max(0, Math.min(THEMES.length - 1, Integer.parseInt(p.getProperty("theme", "0").trim())));
        } catch (IOException ignored) { } catch (NumberFormatException ignored) { }
    }

    private static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("style", blueMenu ? "blue" : "vanilla");
            p.setProperty("theme", Integer.toString(theme));
            FileOutputStream out = new FileOutputStream(file());
            p.store(out, "Blue Client main menu");
            out.close();
        } catch (IOException ignored) { }
    }

    public static boolean blueMenu() { load(); return blueMenu; }
    public static void setBlueMenu(boolean v) { load(); blueMenu = v; save(); }
    public static int theme() { load(); return theme; }
    public static void setTheme(int t) { load(); theme = t; save(); }
}
