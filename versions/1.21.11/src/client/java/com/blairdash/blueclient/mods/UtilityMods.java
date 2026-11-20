package com.blairdash.blueclient.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Utility mods: waypoints, radar, auto text. */
final class UtilityMods {
    private UtilityMods() { }

    static void register(List<BlueMod> out) {
        out.add(new Waypoints());
        out.add(new Radar());
        out.add(new AutoText());
    }

    // ---- Waypoints ---------------------------------------------------------
    static final class Waypoints extends BlueMod {
        private static final int[] PALETTE = { 0xFF55FFFF, 0xFFFF5555, 0xFF55FF55, 0xFFFFFF55, 0xFFFF55FF, 0xFFFFAA00, 0xFF5555FF, 0xFFFFFFFF };
        private record Wp(String dim, String name, int x, int y, int z, int color) { }
        private final List<Wp> wps = new ArrayList<>();
        private final KeyBinding add = HudMods.key("waypoint_add", 66);
        private final KeyBinding removeNearest = HudMods.key("waypoint_remove", 78);
        private final KeyBinding menu = HudMods.key("waypoint_menu", 75);
        private final Setting deathPoint = bool("death", "Auto waypoint on death", true);
        private final Setting markers = bool("markers", "Show markers in the world", true);
        private final Setting listOn = bool("list", "Show list on the HUD", true);
        private final Setting maxShown = num("shown", "Waypoints shown in list", 1, 8, 0, 4);
        private final Setting maxDist = num("max_dist", "Marker range (0 = unlimited)", 0, 20000, 0, 0);
        private boolean loaded, wasDead;

        Waypoints() { super("waypoints", "Waypoints", "UTILITY", Items.COMPASS); enabled = true; }
        private static Path file() {
            String base = System.getenv("LOCALAPPDATA");
            Path dir = base != null ? Path.of(base, "BlueClient") : Path.of(System.getProperty("user.home"), ".blueclient");
            return dir.resolve("waypoints.txt");
        }
        private void load() {
            loaded = true;
            try {
                Path f = file();
                if (!Files.exists(f)) return;
                for (String l : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                    String[] p = l.split("\\|");
                    if (p.length == 5 || p.length == 6) {
                        int col = p.length == 6 ? (int) Long.parseLong(p[5]) : PALETTE[0];
                        wps.add(new Wp(p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]), col));
                    }
                }
            } catch (Throwable t) { System.err.println("[clientmod] waypoints load: " + t); }
        }
        private void save() {
            try {
                Path f = file(); Files.createDirectories(f.getParent());
                List<String> lines = new ArrayList<>();
                for (Wp w : wps) lines.add(w.dim + "|" + w.name.replace('|', '/') + "|" + w.x + "|" + w.y + "|" + w.z + "|" + w.color);
                Files.write(f, lines, StandardCharsets.UTF_8);
            } catch (Throwable t) { System.err.println("[clientmod] waypoints save: " + t); }
        }
        private static String dim(MinecraftClient c) { return c.world.getRegistryKey().getValue().toString(); }
        private static String host(MinecraftClient c) { var s = c.getCurrentServerEntry(); if (s != null) return s.address; var srv = c.getServer(); return srv == null ? "singleplayer" : "sp:" + srv.getSaveProperties().getLevelName(); }
        private static void msg(MinecraftClient c, String s) { c.player.sendMessage(Text.literal(s), true); }
        private static double eyeY(MinecraftClient c) { return c.player.getEyeY(); }
        private static double yawDeg(MinecraftClient c) { return c.player.getYaw(); }
        private static double pitchDeg(MinecraftClient c) { return c.player.getPitch(); }
        private static double fovDeg(MinecraftClient c) { return c.options.getFov().getValue(); }

        private static double[] camera(MinecraftClient c) {
            net.minecraft.client.render.Camera cam = c.gameRenderer.getCamera();
            var p = cam.getCameraPos();
            return new double[] { p.x, p.y, p.z, cam.getYaw(), cam.getPitch() };
        }
        private boolean migrated;
        /** Old builds keyed every singleplayer world as "singleplayer"; move those waypoints into the first world opened. */
        private void migrateLegacy(MinecraftClient c) {
            if (migrated) return;
            String h = host(c);
            if (!h.startsWith("sp:")) return;
            if (!loaded) load();
            migrated = true;
            boolean ch = false;
            for (int i = 0; i < wps.size(); i++) {
                Wp w = wps.get(i);
                if (w.dim.startsWith("singleplayer@")) { wps.set(i, new Wp(h + w.dim.substring("singleplayer".length()), w.name, w.x, w.y, w.z, w.color)); ch = true; }
            }
            if (ch) save();
        }

        private List<Wp> here(MinecraftClient c) {
            migrateLegacy(c);
            String d = host(c) + "@" + dim(c);
            List<Wp> out = new ArrayList<>();
            for (Wp w : wps) if (w.dim.equals(d)) out.add(w);
            out.sort(Comparator.comparingDouble(w -> dist2(c, w)));
            return out;
        }
        private void addHere(MinecraftClient c, String name) {
            String d = host(c) + "@" + dim(c);
            int n = 1;
            if (name == null || name.isBlank()) {
                final int[] cnt = { 0 };
                for (Wp w : wps) if (w.dim.equals(d)) cnt[0]++;
                n = cnt[0] + 1;
                name = "WP " + n;
            }
            int color = PALETTE[(wps.size()) % PALETTE.length];
            wps.add(new Wp(d, name, c.player.getBlockX(), c.player.getBlockY(), c.player.getBlockZ(), color));
            save();
            msg(c, "Waypoint \"" + name + "\" added");
        }

        @Override public void tick(MinecraftClient c) {
            if (!loaded) load();
            if (!enabled) return;
            while (add.wasPressed()) addHere(c, null);
            while (removeNearest.wasPressed()) {
                Wp n = nearest(c);
                if (n != null) { wps.remove(n); save(); msg(c, "Removed " + n.name); }
            }
            while (menu.wasPressed()) {
                if (c.currentScreen == null) { c.setScreen(new WaypointScreen(this)); }
            }
            boolean dead = c.player.isDead() || c.player.getHealth() <= 0;
            if (dead && !wasDead && deathPoint.b) {
                String d = host(c) + "@" + dim(c);
                wps.removeIf(w -> w.name.equals("Death") && w.dim.equals(d));
                wps.add(new Wp(d, "Death", c.player.getBlockX(), c.player.getBlockY(), c.player.getBlockZ(), 0xFFFF5555));
                save();
            }
            wasDead = dead;
        }
        private Wp nearest(MinecraftClient c) {
            List<Wp> h = here(c);
            return h.isEmpty() ? null : h.get(0);
        }
        private static double dist2(MinecraftClient c, Wp w) {
            double dx = w.x + 0.5 - c.player.getX(), dy = w.y - c.player.getY(), dz = w.z + 0.5 - c.player.getZ();
            return dx * dx + dy * dy + dz * dz;
        }

        @Override public boolean isHud() { return true; }
        @Override public int hudW(MinecraftClient c) { return 110; }
        @Override public int hudH(MinecraftClient c) { return 40; }
        @Override public int defaultY(MinecraftClient c, int sw, int sh) { return 180; }

        @Override public void render(DrawContext ctx, MinecraftClient c) {
            if (c.world == null || c.player == null) return;
            List<Wp> here = here(c);
            int sw = Draw.guiW(ctx), sh = Draw.guiH(ctx);
            if (markers.b) drawMarkers(ctx, c, here, sw, sh);
            if (!listOn.b) return;
            int x = x(c, sw, sh), y = y(c, sw, sh);
            if (here.isEmpty()) { Draw.text(ctx, c, "No waypoints - press B to add one", x, y, 0xFFAAAAAA, true); return; }
            for (int i = 0; i < Math.min(here.size(), (int) maxShown.n); i++) {
                Wp w = here.get(i);
                double dx = w.x + 0.5 - c.player.getX(), dz = w.z + 0.5 - c.player.getZ();
                double ang = wrap(Math.toDegrees(Math.atan2(-dx, dz)) - yawDeg(c));
                String arrow = Math.abs(ang) < 22.5 ? "↑" : Math.abs(ang) > 157.5 ? "↓" : ang > 0 ? (ang < 67.5 ? "↗" : ang < 112.5 ? "→" : "↘") : (ang > -67.5 ? "↖" : ang > -112.5 ? "←" : "↙");
                Draw.text(ctx, c, String.format(Locale.ROOT, "%s %s  %dm", arrow, w.name, (int) Math.sqrt(dist2(c, w))), x, y + i * 10, w.color, true);
            }
        }
        private static double wrap(double a) { a %= 360; if (a >= 180) a -= 360; if (a < -180) a += 360; return a; }

        /** Projects each waypoint onto the screen (no world renderer hooks, so it works on every version). */
        private void drawMarkers(DrawContext ctx, MinecraftClient c, List<Wp> here, int sw, int sh) {
            double[] cm = camera(c);
            double ex = cm[0], ey = cm[1], ez = cm[2];
            double yaw = Math.toRadians(cm[3]), pit = Math.toRadians(cm[4]);
            double sy = Math.sin(yaw), cy = Math.cos(yaw), sp = Math.sin(pit), cp = Math.cos(pit);
            double fx = -sy * cp, fy = -sp, fz = cy * cp;
            double rx = -cy, rz = -sy;
            double ux = -sy * sp, uy = cp, uz = cy * sp;
            double tan = Math.tan(Math.toRadians(fovDeg(c)) / 2.0), aspect = (double) sw / Math.max(1, sh);
            for (Wp w : here) {
                double dx = w.x + 0.5 - ex, dy = w.y + 1.0 - ey, dz = w.z + 0.5 - ez;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (maxDist.n > 0 && dist > maxDist.n) continue;
                double z = dx * fx + dy * fy + dz * fz, xr = dx * rx + dz * rz, yu = dx * ux + dy * uy + dz * uz;
                double nx, ny;
                if (z > 0.05) { nx = xr / (z * tan * aspect); ny = yu / (z * tan); }
                else { nx = xr >= 0 ? 2 : -2; ny = 0; }
                boolean on = z > 0.05 && Math.abs(nx) <= 0.96 && Math.abs(ny) <= 0.92;
                if (!on) {
                    double k = Math.max(Math.abs(nx) / 0.96, Math.abs(ny) / 0.92);
                    if (k > 0) { nx /= k; ny /= k; }
                }
                int px = (int) (sw / 2.0 + nx * sw / 2.0), py = (int) (sh / 2.0 - ny * sh / 2.0);
                int r = on ? 3 : 2;
                diamond(ctx, px, py, r + 1, 0xFF000000);
                diamond(ctx, px, py, r, w.color);
                String distS = (int) dist + "m";
                if (on) {
                    String label = w.name;
                    Draw.text(ctx, c, label, px - Draw.width(c, label) / 2, py - 15, w.color, true);
                    Draw.text(ctx, c, distS, px - Draw.width(c, distS) / 2, py + 7, 0xFFDDDDDD, true);
                } else {
                    Draw.text(ctx, c, distS, px - Draw.width(c, distS) / 2, py + 7, 0xFFDDDDDD, true);
                }
            }
        }
        private static void diamond(DrawContext ctx, int cx, int cy, int r, int color) {
            for (int i = -r; i <= r; i++) {
                int half = r - Math.abs(i);
                ctx.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, color);
            }
        }

        /** Menu: name a waypoint, add it at your position, recolour or delete existing ones. */
        static final class WaypointScreen extends net.minecraft.client.gui.screen.Screen {
            private final Waypoints owner;
            private net.minecraft.client.gui.widget.TextFieldWidget nameBox;
            private String typed = "";
            private int page;
            private static final int PER_PAGE = 7;

            WaypointScreen(Waypoints owner) { super(Text.literal("Waypoints")); this.owner = owner; }

            @Override protected void init() {
                int cx = width / 2;
                MinecraftClient mc = MinecraftClient.getInstance();
                nameBox = new net.minecraft.client.gui.widget.TextFieldWidget(textRenderer, cx - 130, 34, 150, 20, Text.literal("Name"));
                nameBox.setMaxLength(24);
                nameBox.setText(typed);
                addDrawableChild(nameBox);
                addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal("Add here"), b -> {
                    owner.addHere(mc, nameBox.getText().trim());
                    typed = "";
                    rebuild();
                }).dimensions(cx + 26, 34, 104, 20).build());
                List<Wp> list = owner.here(mc);
                int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
                if (page >= pages) page = pages - 1;
                for (int i = 0; i < PER_PAGE; i++) {
                    int idx = page * PER_PAGE + i;
                    if (idx >= list.size()) break;
                    Wp w = list.get(idx);
                    int y = 64 + i * 22;
                    int rgb = w.color & 0xFFFFFF;
                    addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal("■").styled(s -> s.withColor(rgb)), b -> {
                        int next = 0;
                        for (int k = 0; k < PALETTE.length; k++) if (PALETTE[k] == w.color) next = (k + 1) % PALETTE.length;
                        int at = owner.wps.indexOf(w);
                        if (at >= 0) owner.wps.set(at, new Wp(w.dim, w.name, w.x, w.y, w.z, PALETTE[next]));
                        owner.save();
                        rebuild();
                    }).dimensions(cx - 130, y, 20, 20).build());
                    addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal("X"), b -> { owner.wps.remove(w); owner.save(); rebuild(); })
                            .dimensions(cx + 110, y, 20, 20).build());
                }
                int by = 64 + PER_PAGE * 22 + 4;
                addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal("<"), b -> { if (page > 0) { page--; rebuild(); } }).dimensions(cx - 130, by, 30, 20).build());
                addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal(">"), b -> { page++; rebuild(); }).dimensions(cx + 100, by, 30, 20).build());
                addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(cx - 40, by, 80, 20).build());
            }
            private void rebuild() { typed = nameBox == null ? "" : nameBox.getText(); clearChildren(); init(); }

            @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
                super.render(ctx, mouseX, mouseY, delta);
                MinecraftClient mc = MinecraftClient.getInstance();
                int cx = width / 2;
                String title = "Waypoints";
                Draw.text(ctx, mc, title, cx - Draw.width(mc, title) / 2, 14, 0xFFFFFFFF, true);
                List<Wp> list = owner.here(mc);
                if (list.isEmpty()) {
                    String s = "No waypoints in this dimension yet.";
                    Draw.text(ctx, mc, s, cx - Draw.width(mc, s) / 2, 100, 0xFFAAAAAA, true);
                }
                for (int i = 0; i < PER_PAGE; i++) {
                    int idx = page * PER_PAGE + i;
                    if (idx >= list.size()) break;
                    Wp w = list.get(idx);
                    String label = String.format(Locale.ROOT, "%s   %d, %d, %d   (%dm)", w.name, w.x, w.y, w.z, (int) Math.sqrt(dist2(mc, w)));
                    Draw.text(ctx, mc, label, cx - 104, 64 + i * 22 + 6, w.color, true);
                }
                String hint = "B = quick add    N = remove nearest    K = this menu";
                Draw.text(ctx, mc, hint, cx - Draw.width(mc, hint) / 2, 64 + PER_PAGE * 22 + 30, 0xFF888888, true);
            }
            @Override public boolean shouldPause() { return false; }
        }
    }

    // ---- Radar -------------------------------------------------------------
    static final class Radar extends BlueMod {
        private final Setting size = num("size", "Size", 40, 120, 0, 64);
        private final Setting range = num("range", "Range (blocks)", 10, 100, 0, 32);
        private final Setting mobs = bool("mobs", "Show hostile mobs", true);
        Radar() { super("radar", "Radar", "HUD", Items.RECOVERY_COMPASS); }
        @Override public boolean isHud() { return true; }
        @Override public int hudW(MinecraftClient c) { return (int) size.n; }
        @Override public int hudH(MinecraftClient c) { return (int) size.n; }
        @Override public int defaultX(MinecraftClient c, int sw, int sh) { return sw - (int) size.n - 6; }
        @Override public int defaultY(MinecraftClient c, int sw, int sh) { return 90; }
        @Override public void render(DrawContext ctx, MinecraftClient c) {
            int s = (int) size.n, sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
            int x = x(c, sw, sh), y = y(c, sw, sh), cx = x + s / 2, cy = y + s / 2;
            ctx.fill(x, y, x + s, y + s, 0x66000000);
            ctx.fill(cx - 1, y, cx, y + s, 0x33FFFFFF); ctx.fill(x, cy - 1, x + s, cy, 0x33FFFFFF);
            double yaw = Math.toRadians(c.player.getYaw()), cos = Math.cos(yaw), sin = Math.sin(yaw);
            for (Entity e : c.world.getEntities()) {
                if (e == c.player) continue;
                boolean pl = e instanceof PlayerEntity, mob = mobs.b && e instanceof HostileEntity;
                if (!pl && !mob) continue;
                double dx = e.getX() - c.player.getX(), dz = e.getZ() - c.player.getZ();
                double rx = dx * cos + dz * sin, rz = -dx * sin + dz * cos;
                double px = rx / range.n * (s / 2.0), pz = rz / range.n * (s / 2.0);
                if (Math.abs(px) > s / 2.0 - 2 || Math.abs(pz) > s / 2.0 - 2) continue;
                ctx.fill(cx + (int) px - 1, cy + (int) pz - 1, cx + (int) px + 1, cy + (int) pz + 1, pl ? 0xFFFFFF55 : 0xFFFF5555);
            }
            ctx.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFF3D8BFF);
        }
    }

    // ---- Auto text ---------------------------------------------------------
    static final class AutoText extends BlueMod {
        private static final String[] MSGS = { "gg", "gl hf", "good fight", "ez", "thanks!", "brb", "afk", "wait", "help!", "/spawn", "/home", "/server lobby", "/server smp", "/server duels" };
        private final Setting m1 = choice("m1", "Message 1", 0, MSGS);
        private final Setting m2 = choice("m2", "Message 2", 1, MSGS);
        private final Setting m3 = choice("m3", "Message 3", 9, MSGS);
        private final KeyBinding k1 = HudMods.key("autotext_1", org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
        private final KeyBinding k2 = HudMods.key("autotext_2", org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
        private final KeyBinding k3 = HudMods.key("autotext_3", org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
        AutoText() { super("auto_text", "Auto Text", "UTILITY", Items.WRITABLE_BOOK); }
        @Override public void tick(MinecraftClient c) {
            if (c.player == null) return;
            fire(c, k1, m1); fire(c, k2, m2); fire(c, k3, m3);
        }
        private void fire(MinecraftClient c, KeyBinding k, Setting s) {
            while (k.wasPressed()) {
                if (!enabled || c.currentScreen != null) continue;
                String m = MSGS[Math.max(0, Math.min(s.idx, MSGS.length - 1))];
                if (m.startsWith("/")) c.player.networkHandler.sendChatCommand(m.substring(1));
                else c.player.networkHandler.sendChatMessage(m);
            }
        }
    }
}
