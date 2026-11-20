package com.blairdash.blueclient.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Microsoft / Xbox / Minecraft sign-in for the in-game account switcher. It shares the Blue Client launcher's
 * account store (%APPDATA%\mc-client-launcher\config.json), so adding, removing or switching an account here shows
 * up in the launcher and the other way round. Refresh tokens stay inside that file and are never logged or shown.
 *
 * Plain Java 8 with Gson only, so the same source is used for every Minecraft version.
 */
public final class AccountManager {
    private AccountManager() { }

    private static final String CLIENT_ID = "eabee670-de00-4aed-a4e7-2bd10419c9f3";
    private static final String SCOPE = "XboxLive.signin offline_access";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String MS_DEVICE = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String MS_TOKEN = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String XBL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_LOGIN = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_PROFILE = "https://api.minecraftservices.com/minecraft/profile";
    private static final String MC_ENT = "https://api.minecraftservices.com/entitlements/mcstore";

    /** A signed-in game session (what the game needs to play as an account). */
    public static final class Session {
        public final String username, uuid, accessToken; // uuid without dashes
        Session(String username, String uuid, String accessToken) { this.username = username; this.uuid = uuid; this.accessToken = accessToken; }
    }

    public interface Callback { void done(Session session, String error); }

    /** Progress of an add-account login, polled by the UI. */
    public static final class Login {
        public volatile String uri = "", code = "", status = "Contacting Microsoft...", error;
        public volatile boolean finished, cancelled;
        public volatile Session session;
        public void cancel() { cancelled = true; }
    }

    /** Opens a page in the default browser (Windows). */
    public static void openUrl(String url) {
        try {
            if (url != null && url.startsWith("https://")) new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
        } catch (IOException | RuntimeException ignored) { }
    }

    /** Puts text on the system clipboard (best effort). */
    public static void copy(String text) {
        try {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(text), null);
        } catch (Throwable ignored) { }
    }

    public static String norm(String uuid) { return uuid == null ? "" : uuid.replace("-", "").toLowerCase(); }

    // ---------------------------------------------------------------- store

    private static File file() {
        String appData = System.getenv("APPDATA");
        return appData == null ? null : new File(new File(appData, "mc-client-launcher"), "config.json");
    }

    private static final Object LOCK = new Object();

    private static JsonObject readRoot() throws IOException {
        File f = file();
        if (f == null) throw new IOException("No launcher folder");
        if (!f.isFile()) return new JsonObject();
        JsonElement e = new JsonParser().parse(new String(Files.readAllBytes(f.toPath()), UTF8));
        return e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }

    private static void writeRoot(JsonObject root) throws IOException {
        File f = file();
        if (f == null) throw new IOException("No launcher folder");
        f.getParentFile().mkdirs();
        File tmp = new File(f.getParentFile(), "config.json.bc-tmp");
        Files.write(tmp.toPath(), root.toString().getBytes(UTF8));
        Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static JsonArray accountsOf(JsonObject root) {
        JsonElement a = root.get("accounts");
        if (a != null && a.isJsonArray()) return a.getAsJsonArray();
        JsonArray n = new JsonArray();
        root.add("accounts", n);
        return n;
    }

    private static void upsert(String uuid, String username, String refreshToken, boolean makeActive) throws IOException {
        synchronized (LOCK) {
            JsonObject root = readRoot();
            JsonArray arr = accountsOf(root);
            JsonObject entry = null;
            for (JsonElement el : arr) {
                if (el.isJsonObject() && norm(str(el.getAsJsonObject(), "uuid")).equals(norm(uuid))) { entry = el.getAsJsonObject(); break; }
            }
            if (entry == null) { entry = new JsonObject(); arr.add(entry); }
            entry.addProperty("uuid", uuid);
            entry.addProperty("username", username);
            if (refreshToken != null) entry.addProperty("refreshToken", refreshToken);
            if (makeActive) root.addProperty("activeAccountUuid", uuid);
            writeRoot(root);
        }
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    /** Removes an account from the shared store; the launcher falls back to another account if it was the active one. */
    public static boolean remove(String uuid) {
        synchronized (LOCK) {
            try {
                JsonObject root = readRoot();
                JsonArray arr = accountsOf(root), keep = new JsonArray();
                String firstLeft = null;
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) continue;
                    String u = str(el.getAsJsonObject(), "uuid");
                    if (norm(u).equals(norm(uuid))) continue;
                    keep.add(el);
                    if (firstLeft == null) firstLeft = u;
                }
                root.add("accounts", keep);
                if (norm(str(root, "activeAccountUuid")).equals(norm(uuid))) {
                    if (firstLeft != null) root.addProperty("activeAccountUuid", firstLeft); else root.remove("activeAccountUuid");
                }
                writeRoot(root);
                LauncherAccounts.invalidate();
                return true;
            } catch (IOException | RuntimeException e) { return false; }
        }
    }

    /** Marks an account active in the shared store without signing in (the launcher uses it on its next launch). */
    public static boolean setActive(String uuid) {
        synchronized (LOCK) {
            try {
                JsonObject root = readRoot();
                root.addProperty("activeAccountUuid", uuid);
                writeRoot(root);
                LauncherAccounts.invalidate();
                return true;
            } catch (IOException | RuntimeException e) { return false; }
        }
    }

    // ------------------------------------------------------------ switching

    /** Refreshes the stored account and returns a fresh game session on a worker thread. */
    public static void switchTo(final String uuid, final Callback cb) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    String refresh = null;
                    synchronized (LOCK) {
                        for (JsonElement el : accountsOf(readRoot())) {
                            if (el.isJsonObject() && norm(str(el.getAsJsonObject(), "uuid")).equals(norm(uuid))) { refresh = str(el.getAsJsonObject(), "refreshToken"); break; }
                        }
                    }
                    if (refresh == null) throw new IOException("That account has no saved sign-in. Add it again.");
                    JsonObject tok = post(MS_TOKEN, form("grant_type", "refresh_token", "client_id", CLIENT_ID, "refresh_token", refresh, "scope", SCOPE), false);
                    if (tok.has("error")) throw new IOException("Sign-in expired. Remove and add the account again.");
                    Session s = chain(str(tok, "access_token"));
                    // Microsoft rotates the refresh token: save the new one right away so the launcher keeps working.
                    upsert(s.uuid, s.username, str(tok, "refresh_token"), true);
                    LauncherAccounts.invalidate();
                    cb.done(s, null);
                } catch (Exception e) {
                    cb.done(null, message(e));
                }
            }
        }, "BlueClient-AccountSwitch");
        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------------- add one

    /** Starts the Microsoft device-code login. The UI shows login.uri + login.code and polls login.finished. */
    public static Login startLogin() {
        final Login login = new Login();
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    JsonObject dc = post(MS_DEVICE, form("client_id", CLIENT_ID, "scope", SCOPE), false);
                    if (dc.has("error")) throw new IOException("Microsoft sign-in is unavailable");
                    login.uri = str(dc, "verification_uri");
                    login.code = str(dc, "user_code");
                    login.status = "Enter the code on the page, then come back.";
                    String device = str(dc, "device_code");
                    int interval = dc.has("interval") ? dc.get("interval").getAsInt() : 5;
                    long deadline = System.currentTimeMillis() + (dc.has("expires_in") ? dc.get("expires_in").getAsLong() : 900) * 1000L;
                    JsonObject tok = null;
                    while (System.currentTimeMillis() < deadline) {
                        Thread.sleep(interval * 1000L);
                        if (login.cancelled) throw new IOException("Cancelled");
                        JsonObject r = post(MS_TOKEN, form("grant_type", "urn:ietf:params:oauth:grant-type:device_code", "client_id", CLIENT_ID, "device_code", device), false);
                        if (!r.has("error")) { tok = r; break; }
                        String err = str(r, "error");
                        if ("authorization_pending".equals(err)) continue;
                        if ("slow_down".equals(err)) { interval += 5; continue; }
                        throw new IOException("Sign-in failed (" + err + ")");
                    }
                    if (tok == null) throw new IOException("The code expired");
                    login.status = "Signing in to Minecraft...";
                    Session s = chain(str(tok, "access_token"));
                    upsert(s.uuid, s.username, str(tok, "refresh_token"), true);
                    LauncherAccounts.invalidate();
                    login.session = s;
                    login.status = "Signed in as " + s.username;
                } catch (Exception e) {
                    login.error = message(e);
                } finally {
                    login.finished = true;
                }
            }
        }, "BlueClient-AccountLogin");
        t.setDaemon(true);
        t.start();
        return login;
    }

    // ------------------------------------------------------------ auth chain

    private static Session chain(String msAccess) throws IOException {
        JsonObject xblBody = new JsonObject();
        JsonObject props = new JsonObject();
        props.addProperty("AuthMethod", "RPS");
        props.addProperty("SiteName", "user.auth.xboxlive.com");
        props.addProperty("RpsTicket", "d=" + msAccess);
        xblBody.add("Properties", props);
        xblBody.addProperty("RelyingParty", "http://auth.xboxlive.com");
        xblBody.addProperty("TokenType", "JWT");
        JsonObject xbl = post(XBL, xblBody.toString(), true);
        if (!xbl.has("Token")) throw new IOException("Xbox Live sign-in failed");
        String uhs = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

        JsonObject xstsBody = new JsonObject();
        JsonObject p2 = new JsonObject();
        p2.addProperty("SandboxId", "RETAIL");
        JsonArray ut = new JsonArray();
        ut.add(new com.google.gson.JsonPrimitive(str(xbl, "Token")));
        p2.add("UserTokens", ut);
        xstsBody.add("Properties", p2);
        xstsBody.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsBody.addProperty("TokenType", "JWT");
        JsonObject xsts = post(XSTS, xstsBody.toString(), true);
        if (!xsts.has("Token")) {
            String x = xsts.has("XErr") ? xsts.get("XErr").getAsString() : "";
            if (x.equals("2148916233")) throw new IOException("This account has no Xbox profile");
            if (x.equals("2148916238")) throw new IOException("This account needs a family group");
            throw new IOException("Xbox sign-in failed");
        }

        JsonObject mcBody = new JsonObject();
        mcBody.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + str(xsts, "Token"));
        JsonObject mc = post(MC_LOGIN, mcBody.toString(), true);
        String access = str(mc, "access_token");
        if (access == null) throw new IOException("Minecraft sign-in failed");

        JsonObject ent = get(MC_ENT, access);
        if (!ent.has("items") || ent.getAsJsonArray("items").size() == 0) throw new IOException("This account doesn't own Minecraft: Java Edition");
        JsonObject prof = get(MC_PROFILE, access);
        String id = str(prof, "id"), name = str(prof, "name");
        if (id == null || name == null) throw new IOException("Couldn't load the Minecraft profile");
        return new Session(name, id, access);
    }

    // ------------------------------------------------------------------ http

    private static String form(String... kv) {
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i + 1 < kv.length; i += 2) {
                if (sb.length() > 0) sb.append('&');
                sb.append(URLEncoder.encode(kv[i], "UTF-8")).append('=').append(URLEncoder.encode(kv[i + 1], "UTF-8"));
            }
        } catch (java.io.UnsupportedEncodingException ignored) { }
        return sb.toString();
    }

    private static JsonObject post(String url, String body, boolean json) throws IOException {
        HttpURLConnection c = open(url);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", json ? "application/json" : "application/x-www-form-urlencoded");
        c.setRequestProperty("Accept", "application/json");
        OutputStream os = c.getOutputStream();
        os.write(body.getBytes(UTF8));
        os.close();
        return read(c);
    }

    private static JsonObject get(String url, String bearer) throws IOException {
        HttpURLConnection c = open(url);
        c.setRequestProperty("Authorization", "Bearer " + bearer);
        c.setRequestProperty("Accept", "application/json");
        return read(c);
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        return c;
    }

    private static JsonObject read(HttpURLConnection c) throws IOException {
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = "";
        if (in != null) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            text = new String(bo.toByteArray(), UTF8);
        }
        try {
            JsonElement e = new JsonParser().parse(text);
            if (e.isJsonObject()) return e.getAsJsonObject();
        } catch (RuntimeException ignored) { }
        JsonObject o = new JsonObject();
        o.addProperty("error", "http_" + code);
        return o;
    }

    private static String message(Exception e) {
        String m = e.getMessage();
        return m == null || m.isEmpty() ? "Something went wrong" : m;
    }
}
