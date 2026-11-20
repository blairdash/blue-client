package com.blairdash.blueclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps the Blue Client launcher up to date from inside the game.
 *
 * Mirrors the launcher's own auto-update: look through the releases of blairdash/blue-client, take the newest
 * latest.yml, and if its version is higher than the installed launcher, download that installer (size and sha512
 * are checked against latest.yml) and run it silently once the game closes. The launcher comes back up
 * afterwards, the same way electron-updater restarts it.
 *
 * Windows only. It never blocks or crashes the game: everything runs on a background thread and every failure
 * is only written to %LOCALAPPDATA%\BlueClient\launcher-updater.log.
 *
 * Opt out with -Dblueclient.noLauncherUpdate=true, the BLUECLIENT_NO_LAUNCHER_UPDATE environment variable, or an
 * empty file %LOCALAPPDATA%\BlueClient\no-launcher-update.
 *
 * Plain Java 8 with no Minecraft or loader classes, so the same file is used for every Minecraft version.
 */
public final class LauncherUpdater {
    private static final String REPO = "blairdash/blue-client";
    private static final String RELEASES_API = "https://api.github.com/repos/" + REPO + "/releases?per_page=30";
    private static final String DOWNLOAD_PREFIX = "https://github.com/" + REPO + "/releases/download/";
    private static final String LAUNCHER_PACKAGE_NAME = "mc-client-launcher";
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_RELEASES_CHECKED = 8;
    private static final long START_DELAY_MS = 20000L;
    private static final long MAX_INSTALLER_BYTES = 400L * 1024 * 1024;

    private static boolean started;
    private static volatile File readyInstaller;
    private static volatile String readyVersion;

    private LauncherUpdater() { }

    /** Call once from the client initializer. Returns immediately. */
    public static synchronized void start() {
        if (started) return;
        started = true;
        try {
            if (!isWindows() || Boolean.getBoolean("fabric.development") || optedOut()) return;
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(START_DELAY_MS);
                    checkAndPrepare();
                } catch (Throwable e) {
                    log("stopped: " + e);
                }
            }, "BlueClient-LauncherUpdater");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        } catch (Throwable ignored) {
            // Never let the updater affect the game.
        }
    }

    /** The whole check. Test hooks: -Dblueclient.launcherUpdate.installedVersion=x.y.z and ...dryRun=true. */
    static void checkAndPrepare() throws Exception {
        File dir = findLauncherDir();
        if (dir == null) { log("Blue Client launcher is not installed here; nothing to update"); return; }
        String installedText = System.getProperty("blueclient.launcherUpdate.installedVersion");
        if (installedText == null) installedText = readInstalledVersion(dir);
        int[] installed = parseVersion(installedText);
        if (installed == null) { log("could not read the installed launcher version in " + dir); return; }

        Installer best = findNewestInstaller();
        if (best == null) { log("no launcher installer found on GitHub (installed " + installedText + ")"); return; }
        if (!isNewer(best.version, installed)) {
            log("launcher " + installedText + " is up to date (newest installer is " + best.versionText + ")");
            return;
        }
        if (attempts(best.versionText) >= MAX_ATTEMPTS) {
            log("launcher " + installedText + " is older than " + best.versionText + " but " + MAX_ATTEMPTS
                    + " installs already failed; giving up");
            return;
        }
        log("launcher " + installedText + " is older than " + best.versionText + "; downloading " + best.url);
        File file = download(best);
        readyInstaller = file;
        readyVersion = best.versionText;
        if (Boolean.getBoolean("blueclient.launcherUpdate.dryRun")) {
            log("dry run: would install " + file + " when the game closes");
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(LauncherUpdater::install, "BlueClient-LauncherUpdater-install"));
        log("downloaded and verified " + file.getName() + "; it will be installed when the game closes");
    }

    /** Runs from a JVM shutdown hook: starts the installer the way electron-updater does, then the game exits. */
    private static void install() {
        try {
            File f = readyInstaller;
            String v = readyVersion;
            if (f == null || !f.isFile()) return;
            countAttempt(v);
            ProcessBuilder pb = new ProcessBuilder(f.getAbsolutePath(), "--updated", "/S", "--force-run");
            pb.directory(f.getParentFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.to(new File("NUL")));
            pb.start();
            log("started the launcher installer for " + v);
        } catch (Throwable e) {
            log("could not start the installer: " + e);
        }
    }

    // --- finding the installed launcher ------------------------------------------------------------

    private static File findLauncherDir() {
        String local = System.getenv("LOCALAPPDATA");
        if (local != null) {
            File d = new File(local, "Programs" + File.separator + "Blue Client");
            if (isLauncherDir(d)) return d;
        }
        String reg = registryInstallLocation();
        if (reg != null) {
            File d = new File(reg);
            if (isLauncherDir(d)) return d;
        }
        return null;
    }

    private static boolean isLauncherDir(File d) {
        return new File(d, "Blue Client.exe").isFile() && new File(d, "resources" + File.separator + "app.asar").isFile();
    }

    /** Fallback for a custom install folder: the uninstall entry the NSIS installer wrote. */
    private static String registryInstallLocation() {
        try {
            String out = run("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                    "/s", "/f", "Blue Client", "/d", "/e");
            String key = null;
            String found = null;
            for (String line : out.split("\\r?\\n")) {
                if (line.startsWith("HKEY_")) {
                    key = line.trim();
                } else if (key != null && line.contains("DisplayName") && line.contains("Blue Client")) {
                    found = key;
                    break;
                }
            }
            if (found == null) return null;
            String loc = run("reg", "query", found, "/v", "InstallLocation");
            Matcher m = Pattern.compile("InstallLocation\\s+REG_SZ\\s+(.+)").matcher(loc);
            return m.find() ? m.group(1).trim() : null;
        } catch (Throwable e) {
            return null;
        }
    }

    /** package.json inside resources\app.asar: 16-byte prefix, JSON header, then the packed files. */
    private static String readInstalledVersion(File dir) {
        File asar = new File(dir, "resources" + File.separator + "app.asar");
        try (RandomAccessFile r = new RandomAccessFile(asar, "r")) {
            byte[] head = new byte[16];
            r.readFully(head);
            long size = le32(head, 4);
            long strLen = le32(head, 12);
            if (strLen <= 0 || strLen > 64L * 1024 * 1024) return null;
            byte[] hj = new byte[(int) strLen];
            r.readFully(hj);
            JsonObject files = new JsonParser().parse(new String(hj, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("files");
            JsonObject pj = files.getAsJsonObject("package.json");
            long off = Long.parseLong(pj.get("offset").getAsString());
            int len = pj.get("size").getAsInt();
            byte[] data = new byte[len];
            r.seek(8 + size + off);
            r.readFully(data);
            JsonObject pkg = new JsonParser().parse(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!LAUNCHER_PACKAGE_NAME.equals(pkg.get("name").getAsString())) return null; // some other app
            return pkg.get("version").getAsString();
        } catch (Throwable e) {
            log("could not read " + asar + ": " + e);
            return null;
        }
    }

    private static long le32(byte[] b, int o) {
        return (b[o] & 0xFFL) | ((b[o + 1] & 0xFFL) << 8) | ((b[o + 2] & 0xFFL) << 16) | ((b[o + 3] & 0xFFL) << 24);
    }

    // --- finding the newest installer on GitHub ----------------------------------------------------

    private static final class Installer {
        int[] version;
        String versionText;
        String path;
        String sha512;
        String url;
        long size;
    }

    private static Installer findNewestInstaller() throws IOException {
        JsonArray releases = new JsonParser().parse(httpText(RELEASES_API)).getAsJsonArray();
        Installer best = null;
        int checked = 0;
        for (JsonElement e : releases) {
            JsonObject r = e.getAsJsonObject();
            if (flag(r, "draft") || flag(r, "prerelease")) continue;
            String ymlUrl = assetUrl(r, "latest.yml");
            if (ymlUrl == null) continue;
            if (++checked > MAX_RELEASES_CHECKED) break;
            Installer in = parseYml(httpText(ymlUrl), r);
            if (in != null && (best == null || isNewer(in.version, best.version))) best = in;
        }
        return best;
    }

    private static Installer parseYml(String yml, JsonObject release) {
        String version = find(yml, "(?m)^version:\\s*['\"]?([0-9][^\\s'\"]*)");
        String path = find(yml, "(?m)^path:\\s*['\"]?([^\\s'\"]+)");
        String sha = find(yml, "(?m)^sha512:\\s*['\"]?([A-Za-z0-9+/=]+)");
        String size = find(yml, "(?m)^\\s+size:\\s*(\\d+)");
        int[] v = parseVersion(version);
        if (v == null || path == null || sha == null || !path.toLowerCase().endsWith(".exe")) return null;
        String url = assetUrl(release, path);
        if (url == null) return null;
        Installer in = new Installer();
        in.version = v;
        in.versionText = version;
        in.path = path;
        in.sha512 = sha;
        in.url = url;
        in.size = size == null ? 0 : Long.parseLong(size);
        return in;
    }

    private static String assetUrl(JsonObject release, String name) {
        JsonElement assets = release.get("assets");
        if (assets == null || !assets.isJsonArray()) return null;
        for (JsonElement a : assets.getAsJsonArray()) {
            JsonObject o = a.getAsJsonObject();
            if (name.equals(o.get("name").getAsString())) {
                String url = o.get("browser_download_url").getAsString();
                return url.startsWith(DOWNLOAD_PREFIX) ? url : null; // only ever fetch from our own releases
            }
        }
        return null;
    }

    // --- download + verify --------------------------------------------------------------------------

    private static File download(Installer in) throws Exception {
        File dir = new File(System.getProperty("java.io.tmpdir"), "BlueClient-LauncherUpdate");
        dir.mkdirs();
        File out = new File(dir, in.path.replaceAll("[^A-Za-z0-9._-]", "_"));
        if (out.isFile() && verify(out, in)) return out;
        File[] old = dir.listFiles();
        if (old != null) for (File f : old) f.delete();
        File part = new File(dir, out.getName() + ".part");
        HttpURLConnection c = open(in.url);
        try (InputStream is = c.getInputStream(); OutputStream os = new FileOutputStream(part)) {
            byte[] buf = new byte[64 * 1024];
            long total = 0;
            int n;
            while ((n = is.read(buf)) > 0) {
                total += n;
                if (total > MAX_INSTALLER_BYTES) throw new IOException("installer is unexpectedly large");
                os.write(buf, 0, n);
            }
        } finally {
            c.disconnect();
        }
        if (!verify(part, in)) {
            part.delete();
            throw new IOException("downloaded installer failed the size/sha512 check");
        }
        if (!part.renameTo(out)) throw new IOException("could not move the installer into place");
        return out;
    }

    private static boolean verify(File f, Installer in) {
        try {
            if (in.size > 0 && f.length() != in.size) return false;
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            try (InputStream is = Files.newInputStream(f.toPath())) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = is.read(buf)) > 0) md.update(buf, 0, n);
            }
            return Base64.getEncoder().encodeToString(md.digest()).equals(in.sha512);
        } catch (Throwable e) {
            return false;
        }
    }

    // --- small helpers ---------------------------------------------------------------------------------

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "BlueClient-Mod-LauncherUpdater");
        c.setRequestProperty("Accept", "application/vnd.github+json, */*");
        int code = c.getResponseCode();
        if (code != 200) throw new IOException("HTTP " + code + " for " + url);
        return c;
    }

    private static String httpText(String url) throws IOException {
        HttpURLConnection c = open(url);
        try (InputStream is = c.getInputStream()) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bo.write(buf, 0, n);
                if (bo.size() > 4 * 1024 * 1024) throw new IOException("response too large");
            }
            return new String(bo.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }

    private static String run(String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try (InputStream is = p.getInputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
        }
        p.waitFor();
        return new String(bo.toByteArray());
    }

    private static boolean flag(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && !e.isJsonNull() && e.getAsBoolean();
    }

    private static String find(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    static int[] parseVersion(String v) {
        if (v == null) return null;
        Matcher m = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)").matcher(v.trim());
        return m.find()
                ? new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)) }
                : null;
    }

    static boolean isNewer(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) if (a[i] != b[i]) return a[i] > b[i];
        return false;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static File dataDir() {
        String local = System.getenv("LOCALAPPDATA");
        if (local == null) return null;
        File d = new File(local, "BlueClient");
        d.mkdirs();
        return d;
    }

    private static boolean optedOut() {
        if (Boolean.getBoolean("blueclient.noLauncherUpdate") || System.getenv("BLUECLIENT_NO_LAUNCHER_UPDATE") != null) {
            return true;
        }
        File d = dataDir();
        return d != null && new File(d, "no-launcher-update").exists();
    }

    /** "version=1.1.1;attempts=2": stops us re-running an installer that keeps failing. */
    private static int attempts(String version) {
        try {
            File d = dataDir();
            if (d == null) return 0;
            File f = new File(d, "launcher-update-state.txt");
            if (!f.isFile()) return 0;
            String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
            String v = find(s, "version=([^;]+)");
            String n = find(s, "attempts=(\\d+)");
            return version.equals(v) && n != null ? Integer.parseInt(n) : 0;
        } catch (Throwable e) {
            return 0;
        }
    }

    private static void countAttempt(String version) {
        try {
            File d = dataDir();
            if (d == null) return;
            String s = "version=" + version + ";attempts=" + (attempts(version) + 1);
            Files.write(new File(d, "launcher-update-state.txt").toPath(), s.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
            // best effort
        }
    }

    private static synchronized void log(String msg) {
        try {
            File d = dataDir();
            if (d == null) return;
            File f = new File(d, "launcher-updater.log");
            if (f.length() > 100 * 1024) f.delete();
            String line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " " + msg + System.lineSeparator();
            Files.write(f.toPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {
            // best effort
        }
    }
}
