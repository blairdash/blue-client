package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Passive, local-only watcher for suspicious movement by OTHER players.
 * Nothing is sent to the server: suspects are only listed on your own HUD.
 * Treat alerts as "worth reporting / watching", never as proof.
 *
 * Checks are deliberately loose (they catch blatant cheating, not subtle):
 *  - Speed: sustained horizontal speed above SPEED_LIMIT blocks/tick.
 *  - Fly/hover: staying airborne AIR_TICKS_TO_FLAG ticks without falling.
 *
 * Known false-positive sources: cobwebs, levitation, lobby flight (some
 * ranks can fly in lobbies). Turn it off in lobbies.
 */
public class CheatWatch {
    private static final double SPEED_LIMIT = 0.75;        // blocks per tick
    private static final int SPEED_STRIKES_TO_FLAG = 20;   // net ticks over the limit
    private static final int AIR_TICKS_TO_FLAG = 50;       // 2.5 seconds
    private static final double AIR_MAX_NET_DROP = 1.5;    // blocks
    private static final double TELEPORT_DELTA = 5.0;      // per-tick jump treated as a teleport
    private static final long FLAG_COOLDOWN_MS = 20000L;   // per player, per check
    private static final long ALERT_LIFETIME_MS = 15000L;
    private static final long FADE_MS = 2000L;
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

    private static final Map<UUID, Tracker> trackers = new HashMap<UUID, Tracker>();
    private static final Deque<Alert> alerts = new ArrayDeque<Alert>();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.cheatWatchEnabled || mc.theWorld == null || mc.thePlayer == null) {
            trackers.clear();
            alerts.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Set<UUID> seen = new HashSet<UUID>();

        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (p == mc.thePlayer) continue;
            UUID id = p.getUniqueID();
            seen.add(id);
            Tracker t = trackers.get(id);
            if (t == null) {
                t = new Tracker();
                trackers.put(id, t);
            }
            update(p, t, now);
        }
        trackers.keySet().retainAll(seen);

        while (!alerts.isEmpty() && now - alerts.peekFirst().time > ALERT_LIFETIME_MS) {
            alerts.pollFirst();
        }
    }

    private static boolean isExempt(EntityPlayer p) {
        return !p.isEntityAlive() || p.ridingEntity != null || p.isPlayerSleeping()
                || p.isSpectator() || p.capabilities.isCreativeMode;
    }

    private static void update(EntityPlayer p, Tracker t, long now) {
        double x = p.posX, y = p.posY, z = p.posZ;
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
        boolean airborne = !p.onGround && !p.isInWater() && !p.isInLava() && !p.isOnLadder();
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

    private static void addAlert(EntityPlayer p, String detail, long now) {
        alerts.addLast(new Alert(p.getName() + ": " + detail, now));
        while (alerts.size() > MAX_ALERTS) alerts.pollFirst();
    }

    @SubscribeEvent
    public void onRenderPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.cheatWatchEnabled || alerts.isEmpty() || mc.gameSettings.hideGUI) return;

        long now = System.currentTimeMillis();
        int centerX = event.resolution.getScaledWidth() / 2;

        String header = "Possible cheating (report, don't accuse)";
        mc.fontRendererObj.drawStringWithShadow(header, centerX - mc.fontRendererObj.getStringWidth(header) / 2, 30, 0xFFAAAAAA);

        int y = 42;
        for (Alert a : alerts) {
            long remaining = ALERT_LIFETIME_MS - (now - a.time);
            float fade = remaining < FADE_MS ? Math.max(0f, remaining / (float) FADE_MS) : 1f;
            int alpha = Math.max(24, (int) (255 * fade));
            int color = (alpha << 24) | 0xFF5555;
            String line = "! " + a.text;
            mc.fontRendererObj.drawStringWithShadow(line, centerX - mc.fontRendererObj.getStringWidth(line) / 2, y, color);
            y += 10;
        }
    }
}
