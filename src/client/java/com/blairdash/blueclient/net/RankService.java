package com.blairdash.blueclient.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Talks to the Blue Client rank backend: sends a heartbeat while you're
 * in a world, and polls who else is currently online so their star color
 * can be drawn next to their nametag. Fully async and best-effort — a
 * network hiccup here must never affect gameplay.
 */
public final class RankService {

    // TODO: set this to wherever you deployed blueclient-rank-backend, e.g. "https://ranks.blairdash.com"
    private static final String BACKEND_URL = "https://ranks.blairdash.com";

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "blueclient-rank-service");
                t.setDaemon(true);
                return t;
            });

    // player UUID -> "#RRGGBB", refreshed every poll. Only holds players the
    // backend currently considers online.
    private static final Map<UUID, String> ONLINE_COLORS = new ConcurrentHashMap<>();

    private static boolean started = false;

    private RankService() {}

    public static synchronized void start() {
        if (started) return;
        started = true;
        SCHEDULER.scheduleWithFixedDelay(RankService::heartbeatTick, 0, 30, TimeUnit.SECONDS);
        SCHEDULER.scheduleWithFixedDelay(RankService::pollTick, 5, 20, TimeUnit.SECONDS);
    }

    /** ARGB color (full alpha) for this player's star, or null if they're not a currently-online Blue Client user. */
    public static Integer colorFor(UUID playerUuid) {
        String hex = ONLINE_COLORS.get(playerUuid);
        if (hex == null || hex.length() != 7) return null;
        try {
            return 0xFF000000 | (Integer.parseInt(hex.substring(1), 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void heartbeatTick() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null) return; // only report while actually in a world

            UUID uuid = client.player.getUuid();
            String username = client.player.getName().getString();
            if (uuid == null || username == null || username.isEmpty()) return;

            JsonObject body = new JsonObject();
            body.addProperty("uuid", uuid.toString());
            body.addProperty("username", username);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND_URL + "/api/heartbeat"))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> null);
        } catch (Exception ignored) {
            // cosmetic feature — never let this affect gameplay
        }
    }

    private static void pollTick() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND_URL + "/api/online"))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(RankService::applyOnlineResponse)
                    .exceptionally(ex -> null);
        } catch (Exception ignored) {
        }
    }

    private static void applyOnlineResponse(HttpResponse<String> response) {
        try {
            if (response.statusCode() != 200) return;
            JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();
            Map<UUID, String> fresh = new ConcurrentHashMap<>();
            for (var el : arr) {
                JsonObject obj = el.getAsJsonObject();
                UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                String color = obj.get("color").getAsString();
                fresh.put(uuid, color);
            }
            ONLINE_COLORS.clear();
            ONLINE_COLORS.putAll(fresh);
        } catch (Exception ignored) {
            // malformed/unexpected response — keep the previous cache instead of wiping it
        }
    }
}