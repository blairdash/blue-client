package com.blairdash.blueclient.mods;

import com.blairdash.blueclient.net.RankService;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Features specific to the Blue SMP network. */
final class BlueMods {
    private BlueMods() { }

    static void register(List<BlueMod> out) {
        out.add(new SmpStatus());
        out.add(new QuickJoin());
        out.add(new NearbyRanks());
        out.add(new CheatWatchPlus());
    }

    // ---- SMP status --------------------------------------------------------
    static final class SmpStatus extends TextHudMod {
        private final Setting host = choice("host", "Server", 0, "blue-smp.com");
        private volatile String online = "...", motd = ""; private volatile int ping = -1;
        private volatile boolean up; private long next;
        SmpStatus() { super("smp_status", "Blue SMP Status", "BLUE", Items.BEACON, "Blue SMP: 00 online (00ms)"); }
        @Override public void tick(MinecraftClient c) {
            if (!enabled) return;
            long now = System.currentTimeMillis();
            if (now < next) return;
            next = now + 30_000;
            Thread t = new Thread(() -> query("blue-smp.com", 25565), "BlueSMP-status"); t.setDaemon(true); t.start();
        }
        private void query(String h, int port) {
            try (Socket s = new Socket()) {
                long t0 = System.nanoTime();
                s.connect(new InetSocketAddress(h, port), 3000); s.setSoTimeout(3000);
                DataOutputStream out = new DataOutputStream(s.getOutputStream());
                DataInputStream in = new DataInputStream(s.getInputStream());
                ByteArrayOutputStream hs = new ByteArrayOutputStream(); DataOutputStream hd = new DataOutputStream(hs);
                hd.writeByte(0); varInt(hd, 767); byte[] hb = h.getBytes(StandardCharsets.UTF_8); varInt(hd, hb.length); hd.write(hb); hd.writeShort(port); varInt(hd, 1);
                varInt(out, hs.size()); out.write(hs.toByteArray());
                out.writeByte(1); out.writeByte(0); out.flush();
                readVar(in); readVar(in); int len = readVar(in);
                byte[] data = new byte[len]; in.readFully(data);
                ping = (int) ((System.nanoTime() - t0) / 1_000_000L);
                String json = new String(data, StandardCharsets.UTF_8);
                Matcher m = Pattern.compile("\"online\"\\s*:\\s*(\\d+)").matcher(json);
                Matcher mx = Pattern.compile("\"max\"\\s*:\\s*(\\d+)").matcher(json);
                online = (m.find() ? m.group(1) : "?") + (mx.find() ? "/" + mx.group(1) : "");
                up = true;
            } catch (Throwable e) { up = false; online = "offline"; ping = -1; }
        }
        private static void varInt(DataOutputStream o, int v) throws java.io.IOException {
            while ((v & ~0x7F) != 0) { o.writeByte((v & 0x7F) | 0x80); v >>>= 7; } o.writeByte(v);
        }
        private static int readVar(DataInputStream in) throws java.io.IOException {
            int v = 0, p = 0, b;
            do { b = in.readUnsignedByte(); v |= (b & 0x7F) << p; p += 7; } while ((b & 0x80) != 0 && p < 35);
            return v;
        }
        @Override protected List<String> lines(MinecraftClient c) {
            return List.of(up ? "Blue SMP: " + online + " online (" + ping + "ms)" : "Blue SMP: " + online);
        }
    }

    // ---- Quick join --------------------------------------------------------
    static final class QuickJoin extends BlueMod {
        private final net.minecraft.client.option.KeyBinding key = HudMods.key("quick_join", org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
        QuickJoin() { super("quick_join", "Quick Join", "BLUE", Items.ENDER_PEARL); }
        /** Polled from the title screen via {@link ModManager#tickAlways}. */
        @Override public void tick(MinecraftClient c) { check(c); }
        @Override public void tickMenu(MinecraftClient c) { check(c); }
        void check(MinecraftClient c) {
            if (!enabled) return;
            if (key.wasPressed()) {
                if (c.world != null) return;
                ConnectScreen.connect(new TitleScreen(), c, ServerAddress.parse("blue-smp.com"),
                        new ServerInfo("Blue SMP", "blue-smp.com", ServerInfo.ServerType.OTHER), false, null);
            }
        }
    }

    // ---- Nearby ranks ------------------------------------------------------
    static final class NearbyRanks extends BlueMod {
        private final Setting range = num("range", "Range (blocks)", 8, 128, 0, 48);
        private final Setting max = num("max", "Max players shown", 3, 15, 0, 8);
        NearbyRanks() { super("nearby_ranks", "Nearby Ranks", "BLUE", Items.PLAYER_HEAD); }
        @Override public boolean isHud() { return true; }
        @Override public int hudW(MinecraftClient c) { return 100; }
        @Override public int hudH(MinecraftClient c) { return 50; }
        @Override public int defaultY(MinecraftClient c, int sw, int sh) { return 120; }
        @Override public void render(DrawContext ctx, MinecraftClient c) {
            if (c.world == null || c.player == null) return;
            List<PlayerEntity> near = new ArrayList<>();
            for (PlayerEntity p : c.world.getPlayers()) if (p != c.player && p.distanceTo(c.player) <= range.n) near.add(p);
            near.sort(Comparator.comparingDouble(p -> p.distanceTo(c.player)));
            int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
            int x = x(c, sw, sh), y = y(c, sw, sh), i = 0;
            for (PlayerEntity p : near) {
                if (i >= (int) max.n) break;
                Integer col = RankService.colorFor(p.getUuid());
                int rgb = col == null ? 0xFFFFFFFF : (0xFF000000 | col);
                ctx.drawTextWithShadow(c.textRenderer, p.getName().getString() + " " + (int) p.distanceTo(c.player) + "m", x, y + i * 10, rgb);
                i++;
            }
        }
    }

    // ---- Cheat Watch+ ------------------------------------------------------
    static final class CheatWatchPlus extends BlueMod {
        private final Setting sens = choice("sens", "Sensitivity", 1, "Low (fewer false flags)", "Normal", "High");
        private final Setting step = bool("step", "Check step / high jump", true);
        private final Setting jesus = bool("jesus", "Check walking on water", true);
        private final Setting snap = bool("snap", "Check head snapping", true);
        private final Setting sound = bool("sound", "Play sound on flag", true);
        private final Setting chat = bool("chat", "Print flags in chat", false);
        private final Setting list = bool("list", "Show suspects list", true);

        private static final class T { double x, y, z; float yaw; boolean has; int water, snaps, offGroundJumpTicks; long cd; }
        private final Map<UUID, T> track = new HashMap<>();
        private final Map<String, Integer> flags = new LinkedHashMap<>();
        private final Deque<String> recent = new ArrayDeque<>();
        private final Deque<Long> recentTime = new ArrayDeque<>();

        CheatWatchPlus() { super("cheat_watch_plus", "Cheat Watch+", "BLUE", Items.SPYGLASS); }
        @Override public boolean isHud() { return true; }
        @Override public int hudW(MinecraftClient c) { return 140; }
        @Override public int hudH(MinecraftClient c) { return 50; }
        @Override public int defaultX(MinecraftClient c, int sw, int sh) { return sw - 150; }
        @Override public int defaultY(MinecraftClient c, int sw, int sh) { return 30; }

        private double f() { return sens.idx == 0 ? 1.35 : sens.idx == 2 ? 0.8 : 1.0; }

        @Override public void tick(MinecraftClient c) {
            if (!enabled) { track.clear(); return; }
            long now = System.currentTimeMillis();
            while (!recentTime.isEmpty() && now - recentTime.peekFirst() > 20_000) { recentTime.pollFirst(); recent.pollFirst(); }
            Set<UUID> seen = new HashSet<>();
            for (PlayerEntity p : c.world.getPlayers()) {
                if (p == c.player) continue;
                seen.add(p.getUuid());
                T t = track.computeIfAbsent(p.getUuid(), k -> new T());
                double dx = p.getX() - t.x, dy = p.getY() - t.y, dz = p.getZ() - t.z;
                float dyaw = Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(p.getYaw() - t.yaw));
                boolean had = t.has;
                t.x = p.getX(); t.y = p.getY(); t.z = p.getZ(); t.yaw = p.getYaw(); t.has = true;
                if (!had) continue;
                boolean exempt = !p.isAlive() || p.hasVehicle() || p.isGliding() || p.isSpectator() || p.isInCreativeMode()
                        || p.isClimbing() || Math.sqrt(dx * dx + dy * dy + dz * dz) > 5;
                if (exempt) { t.water = 0; t.snaps = 0; continue; }

                if (step.b && p.isOnGround() && dy > 1.3 * f() && dy < 3) flag(p, t, "Step/HighJump (+" + String.format(Locale.ROOT, "%.1f", dy) + ")", now);

                if (jesus.b) {
                    BlockPos below = BlockPos.ofFloored(p.getX(), p.getY() - 0.2, p.getZ());
                    boolean onWater = c.world.getBlockState(below).isOf(Blocks.WATER) && !p.isTouchingWater() && !p.isSwimming();
                    boolean moving = Math.hypot(dx, dz) > 0.15;
                    if (onWater && moving && c.world.getBlockState(below.up()).isAir()) { if (++t.water > 25 * f()) { t.water = 0; flag(p, t, "Walking on water", now); } }
                    else t.water = Math.max(0, t.water - 1);
                }

                if (snap.b) {
                    if (dyaw > 100) { if (++t.snaps >= 6 * f()) { t.snaps = 0; flag(p, t, "Head snapping", now); } }
                    else if (t.snaps > 0 && (c.world.getTime() & 7) == 0) t.snaps--;
                }
            }
            track.keySet().retainAll(seen);
        }

        private void flag(PlayerEntity p, T t, String what, long now) {
            if (now - t.cd < 15_000) return;
            t.cd = now;
            String n = p.getName().getString();
            flags.merge(n, 1, Integer::sum);
            recent.addLast(n + ": " + what); recentTime.addLast(now);
            while (recent.size() > 5) { recent.pollFirst(); recentTime.pollFirst(); }
            MinecraftClient c = MinecraftClient.getInstance();
            if (sound.b && c.player != null) c.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.6f, 0.7f);
            if (chat.b && c.player != null) c.player.sendMessage(Text.literal("[CheatWatch+] " + n + " - " + what), false);
        }

        @Override public void render(DrawContext ctx, MinecraftClient c) {
            if (!list.b) return;
            int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
            int x = x(c, sw, sh), y = y(c, sw, sh), row = 0;
            for (String s : recent) ctx.drawTextWithShadow(c.textRenderer, s, x, y + row++ * 10, 0xFFFF5555);
            List<Map.Entry<String, Integer>> top = new ArrayList<>(flags.entrySet());
            top.sort((a, b) -> b.getValue() - a.getValue());
            for (int i = 0; i < Math.min(3, top.size()); i++)
                ctx.drawTextWithShadow(c.textRenderer, top.get(i).getKey() + " x" + top.get(i).getValue(), x, y + row++ * 10, 0xFFFFAA00);
        }
    }
}
