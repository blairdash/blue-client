package com.blairdash.blueclient.update;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Self-updater for the standalone Blue Client jar. Runs before the mod initialises (Fabric preLaunch /
 * Forge mod constructor / 1.8.9 preInit). It reads the installed jar's name
 * (blueclient-<version>-<mc>-<fabric|forge>.jar in the mods folder), asks the CDN for the newest build
 * of that loader + Minecraft version, downloads it next to the old jar as *.update, and swaps the files
 * once the game has exited (the running jar is locked, so it can't be replaced in-session).
 * Does nothing when the Blue launcher runs the game (it updates the mod itself), when a dev build is
 * running, or when mods/blueclient-no-update exists.
 */
public final class BlueUpdater {
    private static final String CDN = "https://cdn.blue-client.com";
    private static final Pattern NAME = Pattern.compile("^blueclient-(\\d+(?:\\.\\d+)*)-(.+)-(fabric|forge)\\.jar$", Pattern.CASE_INSENSITIVE);
    private static boolean ran;

    private BlueUpdater() { }

    public static synchronized void run() {
        if (ran) return;
        ran = true;
        try {
            if (System.getProperty("blueclient.managed") != null || System.getenv("BLUECLIENT_MANAGED") != null) return;
            Path mods = Paths.get(System.getProperty("blueclient.modsDir", "mods")).toAbsolutePath();
            if (!Files.isDirectory(mods) || Files.exists(mods.resolve("blueclient-no-update"))) return;
            Path self = null; Matcher sm = null;
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(mods, "blueclient-*.jar")) {
                for (Path p : ds) {
                    Matcher m = NAME.matcher(p.getFileName().toString());
                    if (m.matches()) { self = p; sm = m; break; }
                }
            }
            if (self == null) return;                       // not a standalone install (dev build, launcher copy, renamed jar)
            String current = sm.group(1), mc = sm.group(2), loader = sm.group(3).toLowerCase();
            String folder = "standalone" + loader + mc.replace('.', '_');
            String manifest = http(CDN + "/" + folder + "/manifest.json");
            String latest = field(manifest, "version"), file = field(manifest, "file"), sha = field(manifest, "sha256");
            if (latest == null || file == null || compare(latest, current) <= 0) return;
            Path update = mods.resolve(file + ".update");
            byte[] jar = bytes(CDN + "/" + folder + "/" + file.replace(" ", "%20"));
            if (sha != null && !sha.equalsIgnoreCase(hex(MessageDigest.getInstance("SHA-256").digest(jar)))) return;
            Files.write(update, jar);
            scheduleSwap(self, update, mods.resolve(file));
            System.out.println("[BlueClient] Update " + current + " -> " + latest + " downloaded; it installs when the game closes.");
        } catch (Throwable t) {
            System.out.println("[BlueClient] Update check skipped: " + t);   // never block the game
        }
    }

    /** After the JVM exits: delete the old jar and rename the downloaded one into place. */
    private static void scheduleSwap(final Path oldJar, final Path update, final Path target) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
                    ProcessBuilder pb;
                    if (win) {
                        pb = new ProcessBuilder("cmd", "/c", "ping -n 6 127.0.0.1 >nul & del /f /q \"" + oldJar + "\" & move /y \"" + update + "\" \"" + target + "\"");
                    } else {
                        pb = new ProcessBuilder("sh", "-c", "sleep 5; rm -f '" + oldJar + "'; mv -f '" + update + "' '" + target + "'");
                    }
                    pb.start();
                } catch (Throwable ignored) { /* update stays as .update and is retried next launch */ }
            }
        }, "BlueClient-update-swap"));
    }

    private static byte[] bytes(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(5000); c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "BlueClient-Updater");
        if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode() + " for " + url);
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[16384]; int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    private static String http(String url) throws IOException {
        String s = new String(bytes(url), StandardCharsets.UTF_8);
        return s.length() > 0 && s.charAt(0) == '\uFEFF' ? s.substring(1) : s;
    }

    private static String field(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static int compare(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? Integer.parseInt(x[i]) : 0, q = i < y.length ? Integer.parseInt(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    private static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte v : b) s.append(String.format("%02x", v));
        return s.toString();
    }
}