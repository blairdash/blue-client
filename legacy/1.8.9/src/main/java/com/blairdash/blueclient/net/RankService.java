package com.blairdash.blueclient.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** Rank backend client (Java 8 port): heartbeat every 30s, poll /api/online every 20s. Best-effort and fully async. */
public final class RankService {
    private static final String BACKEND_URL = "https://ranks.blairdash.com";
    private static final int TIMEOUT_MS = 5000;
    private static final long STALE_MS = 75000L;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) { Thread t = new Thread(r, "blueclient-rank-service"); t.setDaemon(true); return t; }
    });
    private static final Map<UUID, String> ONLINE_COLORS = new ConcurrentHashMap<UUID, String>();
    private static boolean started;
    private static volatile long lastOk;

    private RankService() { }

    public static synchronized void start() {
        if (started) return;
        started = true;
        SCHEDULER.scheduleWithFixedDelay(new Runnable() { public void run() { heartbeat(); } }, 0, 30, TimeUnit.SECONDS);
        SCHEDULER.scheduleWithFixedDelay(new Runnable() { public void run() { poll(); } }, 5, 20, TimeUnit.SECONDS);
    }

    /** ARGB colour for a currently-online Blue Client user, or null (also null while the backend is down). */
    public static Integer colorFor(UUID uuid) {
        if (uuid == null || System.currentTimeMillis() - lastOk > STALE_MS) return null;
        String hex = ONLINE_COLORS.get(uuid);
        if (hex == null || hex.length() != 7) return null;
        try { return 0xFF000000 | (Integer.parseInt(hex.substring(1), 16) & 0xFFFFFF); } catch (NumberFormatException e) { return null; }
    }

    public static Integer selfColor() {
        try { return colorFor(selfUuid()); } catch (Throwable t) { return null; }
    }

    private static UUID selfUuid() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) return mc.thePlayer.getUniqueID();
        return mc.getSession().getProfile().getId();
    }

    private static void heartbeat() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getSession() == null) return;
            UUID uuid = selfUuid();
            String name = mc.getSession().getUsername();
            if (uuid == null || name == null || name.isEmpty()) return;
            JsonObject body = new JsonObject();
            body.addProperty("uuid", uuid.toString());
            body.addProperty("username", name);
            request("POST", "/api/heartbeat", body.toString());
        } catch (Exception ignored) { }
    }

    private static void poll() {
        try {
            String res = request("GET", "/api/online", null);
            if (res == null) return;
            JsonArray arr = new JsonParser().parse(res).getAsJsonArray();
            Map<UUID, String> fresh = new ConcurrentHashMap<UUID, String>();
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                fresh.put(UUID.fromString(o.get("uuid").getAsString()), o.get("color").getAsString());
            }
            ONLINE_COLORS.clear();
            ONLINE_COLORS.putAll(fresh);
            lastOk = System.currentTimeMillis();
        } catch (Exception ignored) { }
    }

    private static String request(String method, String path, String json) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(BACKEND_URL + path).openConnection();
        try {
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            c.setRequestMethod(method);
            if (json != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                OutputStream os = c.getOutputStream();
                os.write(json.getBytes("UTF-8"));
                os.close();
            }
            if (c.getResponseCode() != 200) return null;
            InputStream in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            return new String(out.toByteArray(), "UTF-8");
        } finally { c.disconnect(); }
    }
}
