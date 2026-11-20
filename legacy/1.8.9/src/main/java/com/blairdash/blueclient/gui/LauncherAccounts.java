package com.blairdash.blueclient.gui;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the account list of the Blue Client launcher (when it is installed) so the main-menu account chip
 * can show them. Only usernames and UUIDs are read -- tokens in the launcher's store are skipped, never kept.
 */
public final class LauncherAccounts {
    private LauncherAccounts() { }

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static final class Account {
        public final String uuid, username;
        public final boolean active;
        Account(String uuid, String username, boolean active) { this.uuid = uuid; this.username = username; this.active = active; }
    }

    private static File file() {
        String appData = System.getenv("APPDATA");
        if (appData == null) return null;
        return new File(new File(appData, "mc-client-launcher"), "config.json");
    }

    private static List<Account> cache = new ArrayList<Account>();
    private static long cacheStamp;

    public static synchronized void invalidate() { cacheStamp = 0; }

    /** The launcher's accounts, or an empty list when the launcher isn't installed. Cached for a couple of seconds. */
    public static synchronized List<Account> list() {
        long now = System.currentTimeMillis();
        if (now - cacheStamp < 2000) return cache;
        cacheStamp = now;
        List<Account> out = new ArrayList<Account>();
        File f = file();
        if (f != null && f.isFile()) {
            JsonReader in = null;
            try {
                in = new JsonReader(new InputStreamReader(new java.io.FileInputStream(f), UTF8));
                List<String[]> found = new ArrayList<String[]>();
                String active = null;
                in.beginObject();
                while (in.hasNext()) {
                    String key = in.nextName();
                    if (key.equals("accounts") && in.peek() == JsonToken.BEGIN_ARRAY) {
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
                    } else if (key.equals("activeAccountUuid") && in.peek() == JsonToken.STRING) {
                        active = in.nextString();
                    } else {
                        in.skipValue();
                    }
                }
                for (String[] a : found) out.add(new Account(a[0], a[1], a[0].equals(active)));
            } catch (IOException e) { out.clear(); }
            catch (RuntimeException e) { out.clear(); }
            finally { if (in != null) try { in.close(); } catch (IOException ignored) { } }
        }
        cache = out;
        return out;
    }

    /** Marks an account as the launcher's active one (used the next time the game is started from the launcher). */
    public static synchronized boolean setActive(String uuid) {
        File f = file();
        if (f == null || uuid == null || !uuid.matches("[0-9a-fA-F-]+")) return false;
        try {
            String text = new String(Files.readAllBytes(f.toPath()), UTF8);
            Matcher m = Pattern.compile("(\"activeAccountUuid\"\\s*:\\s*)\"[^\"]*\"").matcher(text);
            if (!m.find()) return false;
            String patched = m.replaceFirst(Matcher.quoteReplacement(m.group(1) + "\"" + uuid + "\""));
            Files.write(f.toPath(), patched.getBytes(UTF8));
            cacheStamp = 0;
            return true;
        } catch (IOException e) { return false; }
        catch (RuntimeException e) { return false; }
    }
}
