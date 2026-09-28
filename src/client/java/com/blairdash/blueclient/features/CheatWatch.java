package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Passive, local-only watcher for suspicious movement by OTHER players.
 * Nothing is sent to the server: suspects are only listed on your own HUD.
 * Treat alerts as "worth reporting / watching", never as proof.
 *
 * Checks are deliberately loose (they catch blatant cheating, not subtle):
 *  - Speed: sustained horizontal speed above SPEED_LIMIT blocks/tick.
 *  - Fly/hover: staying airborne AIR_TICKS_TO_FLAG ticks without falling.
 *
 * Known false-positive sources: cobwebs, levitation / slow falling, lobby
 * flight (some ranks can fly in lobbies). Turn it off in lobbies.
 */
public class CheatWatch {
    // --- tuning ---
    private static final double SPEED_LIMIT = 0.75;        // blocks per tick
    private static final int SPEED_STRIKES_TO_FLAG = 20;   // net ticks over the limit
    private static final int AIR_TICKS_TO_FLAG = 50;       // 2.5 seconds
    private static final double AIR_MAX_NET_DROP = 1.5;    // blocks
    private static final double TELEPORT_DELTA = 5.0;      // per-tick jump treated as a teleport
    private static final long FLAG_COOLDOWN_MS = 20_000;   // per player, per check
    private static final long ALERT_LIFETIME_MS = 15_000;
    private static final long FADE_MS = 2_000;
    private static final int MAX_ALERTS = 5;

    private static class Tracker {
        boolean hasLast;
        double x, y, z;
        int speedStrikes;
        int airTicks;
        double airStartY;
        long lastSpeedFlag;
        long lastFlyFlag;

        void resetChecks() {
            speedStrikes = 0;
            airTicks = 0;
        }
    }

    private static class Alert {
        final String text;
        final long time;

        Alert(String text, long time) {
            this.text = text;
            this.time = time;
        }
    }

    private static final Map<UUID, Tracker> trackers = new HashMap<>();
    private static final Deque<Alert> alerts = new ArrayDeque<>();

    public static void tick(MinecraftClient client) {
        if (!ModConfig.cheatWatchEnabled || client.world == null || client.player == null) {
            trackers.clear();
            alerts.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Set<UUID> seen = new HashSet<>();

        for (PlayerEntity p : client.world.getPlayers()) {
            if (p == client.player) continue;
            UUID id = p.getUuid();
            seen.add(id);
            update(p, trackers.computeIfAbsent(id, k -> new Tracker()), now);
        }
        trackers.keySet().retainAll(seen);

        while (!alerts.isEmpty() && now - alerts.peekFirst().time > ALERT_LIFETIME_MS) {
            alerts.pollFirst();
        }
    }

    private static boolean isExempt(PlayerEntity p) {
        return !p.isAlive() || p.hasVehicle() || p.isGliding() || p.isUsingRiptide()
                || p.isSleeping() || p.isSpectator() || p.isInCreativeMode();
    }

    private static void update(PlayerEntity p, Tracker t, long now) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        boolean hadLast = t.hasLast;
        double dx = x - t.x, dy = y - t.y, dz = z - t.z;
        t.x = x;
        t.y = y;
        t.z = z;
        t.hasLast = true;
        if (!hadLast) return;

        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double total = Math.sqrt(horizontal * horizontal + dy * dy);

        if (total > TELEPORT_DELTA || isExempt(p)) {
            t.resetChecks();
            return;
        }

        // --- Speed ---
        if (horizontal > SPEED_LIMIT) {
            t.speedStrikes++;
        } else if (t.speedStrikes > 0) {
            t.speedStrikes--;
        }
        if (t.speedStrikes >= SPEED_STRIKES_TO_FLAG) {
            t.speedStrikes = 0;
            if (now - t.lastSpeedFlag > FLAG_COOLDOWN_MS) {
                t.lastSpeedFlag = now;
                addAlert(p, String.format("Speed (%.2f blocks/tick)", horizontal), now);
            }
        }

        // --- Fly / hover ---
        boolean airborne = !p.isOnGround() && !p.isTouchingWater() && !p.isInLava() && !p.isClimbing();
        if (airborne) {
            if (t.airTicks == 0) t.airStartY = y;
            t.airTicks++;
            if (t.airTicks >= AIR_TICKS_TO_FLAG) {
                double net = y - t.airStartY;
                if (net > -AIR_MAX_NET_DROP && now - t.lastFlyFlag > FLAG_COOLDOWN_MS) {
                    t.lastFlyFlag = now;
                    addAlert(p, String.format("Fly (%.1fs in air, dropped %.1f blocks)",
                            t.airTicks / 20.0, Math.max(0, -net)), now);
                }
                t.airTicks = 0; // start a fresh window
            }
        } else {
            t.airTicks = 0;
        }
    }

    private static void addAlert(PlayerEntity p, String detail, long now) {
        alerts.addLast(new Alert(p.getName().getString() + ": " + detail, now));
        while (alerts.size() > MAX_ALERTS) alerts.pollFirst();
    }

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (alerts.isEmpty() || client.options.hudHidden) return;

        long now = System.currentTimeMillis();
        int centerX = ctx.getScaledWindowWidth() / 2;

        ctx.drawCenteredTextWithShadow(client.textRenderer, "Possible cheating (report, don't accuse)",
                centerX, 30, 0xFFAAAAAA);

        int y = 42;
        for (Alert a : alerts) {
            long remaining = ALERT_LIFETIME_MS - (now - a.time);
            float fade = remaining < FADE_MS ? Math.max(0f, remaining / (float) FADE_MS) : 1f;
            int alpha = Math.max(24, (int) (255 * fade));
            int color = (alpha << 24) | 0xFF5555;
            ctx.drawCenteredTextWithShadow(client.textRenderer, "! " + a.text, centerX, y, color);
            y += 10;
        }
    }
}