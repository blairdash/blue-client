package com.blairdash.blueclient.gui;

import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the account list of the Blue Client launcher (when it is installed) so the main-menu account chip can
 * show them. Only usernames and UUIDs are read -- tokens in the launcher's store are skipped, never kept.
 */
public final class LauncherAccounts {
    private LauncherAccounts() { }

    public static final class Account {
        public final String uuid, username;
        public final boolean active;
        Account(String uuid, String username, boolean active) { this.uuid = uuid; this.username = username; this.active = active; }
    }

    private static Path file() {
        String appData = System.getenv("APPDATA");
        if (appData == null) return null;
        return Path.of(appData, "mc-client-launcher", "config.json");
    }

    private static List<Account> cache = new ArrayList<>();
    private static long cacheStamp;

    public static synchronized void invalidate() { cacheStamp = 0; }

    /** The launcher's accounts, or an empty list when the launcher isn't installed. Cached for a couple of seconds. */
    public static synchronized List<Account> list() {
        long now = System.currentTimeMillis();
        if (now - cacheStamp < 2000) return cache;
        cacheStamp = now;
        List<Account> out = new ArrayList<>();
        Path f = file();
        if (f != null && Files.isRegularFile(f)) {
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8); JsonReader in = new JsonReader(r)) {
                List<String[]> found = new ArrayList<>();
                String active = null;
                in.beginObject();
                while (in.hasNext()) {
                    String key = in.nextName();
                    if (key.equals("accounts") && in.peek() == com.google.gson.stream.JsonToken.BEGIN_ARRAY) {
                        in.beginArray();
                        while (in.hasNext()) {
                            String uuid = null, name = null;
                            in.beginObject();
                            while (in.hasNext()) {
                                String k = in.nextName();
                                if (k.equals("uuid")) uuid = in.nextString();
                                else if (k.equals("username")) name = in.nextString();
                                else in.skipValue(); // refreshToken etc. are never read
                            }
                            in.endObject();
                            if (uuid != null && name != null) found.add(new String[] { uuid, name });
                        }
                        in.endArray();
                    } else if (key.equals("activeAccountUuid") && in.peek() == com.google.gson.stream.JsonToken.STRING) {
                        active = in.nextString();
                    } else {
                        in.skipValue();
                    }
                }
                for (String[] a : found) out.add(new Account(a[0], a[1], a[0].equals(active)));
            } catch (IOException | RuntimeException ignored) { out.clear(); }
        }
        cache = out;
        return out;
    }

    /** Marks an account as the launcher's active one (used the next time the game is started from the launcher). */
    public static synchronized boolean setActive(String uuid) {
        Path f = file();
        if (f == null || uuid == null || !uuid.matches("[0-9a-fA-F-]+")) return false;
        try {
            String text = Files.readString(f, StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("(\"activeAccountUuid\"\\s*:\\s*)\"[^\"]*\"").matcher(text);
            if (!m.find()) return false;
            String patched = m.replaceFirst(Matcher.quoteReplacement(m.group(1) + "\"" + uuid + "\""));
            Files.writeString(f, patched, StandardCharsets.UTF_8);
            cacheStamp = 0;
            return true;
        } catch (IOException | RuntimeException e) { return false; }
    }
}