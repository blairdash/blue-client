package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Second batch of ported mods: Radar, Waypoints, Auto Text, TNT Countdown, Horse Stats, Momentum, Mob Size, Nick Hider, Kill Sounds, Audio Subtitles. */
public class MoreMods {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    static KeyBinding wpAdd, wpRemove, at1, at2, at3;

    static void register() {
        ExtraMods.ALL.add(new Radar());
        ExtraMods.ALL.add(new Waypoints());
        ExtraMods.ALL.add(new AutoText());
        ExtraMods.ALL.add(new TntCountdown());
        ExtraMods.ALL.add(new HorseStats());
        ExtraMods.ALL.add(new Momentum());
        ExtraMods.ALL.add(new MobSize());
        ExtraMods.ALL.add(new NickHider());
        ExtraMods.ALL.add(new KillSounds());
        ExtraMods.ALL.add(new AudioSubtitles());
    }

    static void registerKeys() {
        String cat = "key.categories.blueclient";
        wpAdd = new KeyBinding("key.blueclient.waypoint_add", Keyboard.KEY_NONE, cat);
        wpRemove = new KeyBinding("key.blueclient.waypoint_remove", Keyboard.KEY_NONE, cat);
        at1 = new KeyBinding("key.blueclient.autotext_1", Keyboard.KEY_NONE, cat);
        at2 = new KeyBinding("key.blueclient.autotext_2", Keyboard.KEY_NONE, cat);
        at3 = new KeyBinding("key.blueclient.autotext_3", Keyboard.KEY_NONE, cat);
        for (KeyBinding k : new KeyBinding[] { wpAdd, wpRemove, at1, at2, at3 }) ClientRegistry.registerKeyBinding(k);
    }

    private static ExtraMods.Mod find(String id) {
        for (ExtraMods.Mod m : ExtraMods.ALL) if (m.id.equals(id)) return m;
        return null;
    }

    // --- Radar ---------------------------------------------------------------------------

    static final class Radar extends ExtraMods.Mod {
        Radar() { super("radar", "Radar", ExtraMods.HUD, Items.compass, ""); modeLabel = "Size (px)"; modeMax = 120; moreLabels = new String[] { "Range (blocks)" }; moreMax = new int[] { 100 }; }
        int size() { Integer m = ModConfig.extraMode.get(id); return m == null ? 64 : Math.max(40, Math.min(120, m)); }
        int range() { Integer m = ModConfig.extraMode.get(id + "#1"); return m == null ? 32 : Math.max(10, Math.min(100, m)); }
        @Override public int mode() { return size(); }
        @Override public int more(int i) { return range(); }
        @Override public int w(Minecraft c) { return size(); }
        @Override public int h() { return size(); }
        @Override int defX(int sw, Minecraft c) { return sw - size() - 6; }
        @Override int defY(int sh) { return 90; }
        @Override void draw(Minecraft c, int sw, int sh) {
            int s = size(), x = x(sw, c), y = y(sh), cx = x + s / 2, cy = y + s / 2, rng = range();
            Gui.drawRect(x, y, x + s, y + s, 0x66000000);
            Gui.drawRect(cx - 1, y, cx, y + s, 0x33FFFFFF);
            Gui.drawRect(x, cy - 1, x + s, cy, 0x33FFFFFF);
            double yaw = Math.toRadians(c.thePlayer.rotationYaw), cos = Math.cos(yaw), sin = Math.sin(yaw);
            for (Object o : c.theWorld.loadedEntityList) {
                Entity e = (Entity) o;
                if (e == c.thePlayer) continue;
                boolean pl = e instanceof EntityPlayer, mob = e instanceof IMob;
                if (!pl && !mob) continue;
                double dx = e.posX - c.thePlayer.posX, dz = e.posZ - c.thePlayer.posZ;
                double rx = dx * cos + dz * sin, rz = -dx * sin + dz * cos;
                double px = rx / rng * (s / 2.0), pz = rz / rng * (s / 2.0);
                if (Math.abs(px) > s / 2.0 - 2 || Math.abs(pz) > s / 2.0 - 2) continue;
                Gui.drawRect(cx + (int) px - 1, cy + (int) pz - 1, cx + (int) px + 1, cy + (int) pz + 1, pl ? 0xFFFFFF55 : 0xFFFF5555);
            }
            Gui.drawRect(cx - 1, cy - 1, cx + 1, cy + 1, 0xFF3D8BFF);
        }
    }

    // --- Waypoints -----------------------------------------------------------------------

    static final class Waypoints extends ExtraMods.Mod {
        static final class Wp { final String dim, name; final int x, y, z; Wp(String d, String n, int x, int y, int z) { dim = d; name = n; this.x = x; this.y = y; this.z = z; } }
        final List<Wp> wps = new ArrayList<Wp>();
        boolean loaded, wasDead;
        Waypoints() { super("waypoints", "Waypoints", ExtraMods.UTILITY, Items.compass, "^ Home  100m"); modeLabel = "Waypoints shown"; modeMax = 8; ModConfig.extraMode.put(id, 4); }
        @Override public int mode() { Integer m = ModConfig.extraMode.get(id); return m == null ? 4 : Math.max(1, Math.min(8, m)); }
        @Override public int h() { return 40; }
        @Override int defY(int sh) { return 180; }

        File file() {
            String base = System.getenv("LOCALAPPDATA");
            File dir = base != null ? new File(base, "BlueClient") : new File(System.getProperty("user.home"), ".blueclient");
            return new File(dir, "waypoints.txt");
        }
        void load() {
            loaded = true;
            try {
                File f = file();
                if (!f.exists()) return;
                for (String l : Files.readAllLines(f.toPath(), UTF8)) {
                    String[] p = l.split("\\|");
                    if (p.length == 5) wps.add(new Wp(p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4])));
                }
            } catch (Exception e) { System.err.println("[blueclient] waypoints load: " + e); }
        }
        void save() {
            try {
                File f = file(); f.getParentFile().mkdirs();
                List<String> lines = new ArrayList<String>();
                for (Wp w : wps) lines.add(w.dim + "|" + w.name.replace('|', '/') + "|" + w.x + "|" + w.y + "|" + w.z);
                Files.write(f.toPath(), lines, UTF8);
            } catch (Exception e) { System.err.println("[blueclient] waypoints save: " + e); }
        }
        String dim(Minecraft c) {
            String host = c.getCurrentServerData() == null ? "singleplayer" : c.getCurrentServerData().serverIP;
            return host + "@" + c.theWorld.provider.getDimensionId();
        }
        double d2(Minecraft c, Wp w) {
            double dx = w.x + 0.5 - c.thePlayer.posX, dy = w.y - c.thePlayer.posY, dz = w.z + 0.5 - c.thePlayer.posZ;
            return dx * dx + dy * dy + dz * dz;
        }
        @Override void tick(Minecraft c) {
            if (!loaded) load();
            if (!enabled() || c.thePlayer == null || c.theWorld == null) return;
            while (wpAdd.isPressed()) {
                wps.add(new Wp(dim(c), "WP " + (wps.size() + 1), MathHelper.floor_double(c.thePlayer.posX), MathHelper.floor_double(c.thePlayer.posY), MathHelper.floor_double(c.thePlayer.posZ)));
                save();
                c.thePlayer.addChatMessage(new ChatComponentText("Waypoint added at your position"));
            }
            while (wpRemove.isPressed()) {
                Wp n = null; String d = dim(c);
                for (Wp w : wps) if (w.dim.equals(d) && (n == null || d2(c, w) < d2(c, n))) n = w;
                if (n != null) { wps.remove(n); save(); c.thePlayer.addChatMessage(new ChatComponentText("Removed " + n.name)); }
            }
            boolean dead = c.thePlayer.getHealth() <= 0;
            if (dead && !wasDead) {
                final String d = dim(c);
                for (int i = wps.size() - 1; i >= 0; i--) if (wps.get(i).name.equals("Death") && wps.get(i).dim.equals(d)) wps.remove(i);
                wps.add(new Wp(d, "Death", MathHelper.floor_double(c.thePlayer.posX), MathHelper.floor_double(c.thePlayer.posY), MathHelper.floor_double(c.thePlayer.posZ)));
                save();
            }
            wasDead = dead;
        }
        @Override void draw(final Minecraft c, int sw, int sh) {
            if (!loaded) load();
            String d = dim(c);
            List<Wp> here = new ArrayList<Wp>();
            for (Wp w : wps) if (w.dim.equals(d)) here.add(w);
            Collections.sort(here, new Comparator<Wp>() { public int compare(Wp a, Wp b) { return Double.compare(d2(c, a), d2(c, b)); } });
            int x = x(sw, c), y = y(sh);
            for (int i = 0; i < Math.min(here.size(), mode()); i++) {
                Wp w = here.get(i);
                double dx = w.x + 0.5 - c.thePlayer.posX, dz = w.z + 0.5 - c.thePlayer.posZ;
                double ang = MathHelper.wrapAngleTo180_double(Math.toDegrees(Math.atan2(-dx, dz)) - c.thePlayer.rotationYaw);
                String arrow = Math.abs(ang) < 22.5 ? "^" : Math.abs(ang) > 157.5 ? "v" : ang > 0 ? (ang < 67.5 ? "/" : ang < 112.5 ? ">" : "\\") : (ang > -67.5 ? "\\" : ang > -112.5 ? "<" : "/");
                c.fontRendererObj.drawStringWithShadow(arrow + " " + w.name + "  " + (int) Math.sqrt(d2(c, w)) + "m", x, y + i * 10, 0xFF55FFFF);
            }
        }
    }

    // --- Auto Text -----------------------------------------------------------------------

    static final String[] MSGS = { "gg", "gl hf", "good fight", "ez", "thanks!", "brb", "afk", "wait", "help!", "/spawn", "/home", "/server lobby", "/server smp", "/server duels" };

    static final class AutoText extends ExtraMods.Mod {
        AutoText() {
            super("auto_text", "Auto Text", ExtraMods.UTILITY, Items.writable_book, "");
            modeLabel = "Message 1 (0 gg, 1 gl hf, 2 good fight, 3 ez, 4 thanks, 5 brb, 6 afk, 7 wait, 8 help, 9 /spawn, 10 /home, 11-13 /server lobby|smp|duels)";
            modeMax = MSGS.length - 1;
            moreLabels = new String[] { "Message 2", "Message 3" };
            moreMax = new int[] { MSGS.length - 1, MSGS.length - 1 };
        }
        @Override public boolean hasPosition() { return false; }
        @Override public int mode() { Integer m = ModConfig.extraMode.get(id); return m == null ? 0 : Math.max(0, Math.min(MSGS.length - 1, m)); }
        @Override public int more(int i) { Integer m = ModConfig.extraMode.get(id + "#" + i); return m == null ? (i == 1 ? 1 : 9) : Math.max(0, Math.min(MSGS.length - 1, m)); }
        @Override void tick(Minecraft c) {
            if (c.thePlayer == null) return;
            fire(c, at1, mode()); fire(c, at2, more(1)); fire(c, at3, more(2));
        }
        void fire(Minecraft c, KeyBinding k, int idx) {
            while (k.isPressed()) {
                if (!enabled() || c.currentScreen != null) continue;
                c.thePlayer.sendChatMessage(MSGS[idx]);
            }
        }
        @Override void draw(Minecraft c, int sw, int sh) { }
    }

    // --- TNT Countdown / Horse Stats / Momentum ------------------------------------------

    static final class TntCountdown extends ExtraMods.Mod {
        TntCountdown() { super("tnt_countdown", "TNT Countdown", ExtraMods.HUD, Items.gunpowder, "TNT: 4.0s"); }
        @Override List<String> lines(Minecraft c) {
            if (c.theWorld == null) return null;
            EntityTNTPrimed best = null;
            for (Object o : c.theWorld.loadedEntityList)
                if (o instanceof EntityTNTPrimed && (best == null || ((Entity) o).getDistanceToEntity(c.thePlayer) < best.getDistanceToEntity(c.thePlayer))) best = (EntityTNTPrimed) o;
            if (best == null || best.getDistanceToEntity(c.thePlayer) > 64) return null;
            return Collections.singletonList(String.format("TNT: %.1fs (%dm)", best.fuse / 20.0, (int) best.getDistanceToEntity(c.thePlayer)));
        }
    }

    static final class HorseStats extends ExtraMods.Mod {
        HorseStats() { super("horse_stats", "Horse Stats", ExtraMods.HUD, Items.saddle, "Speed: 0.0 b/s\nJump: 0.0 blocks\nHealth: 0"); }
        @Override List<String> lines(Minecraft c) {
            MovingObjectPosition r = c.objectMouseOver;
            if (r == null || !(r.entityHit instanceof EntityHorse)) return null;
            EntityHorse h = (EntityHorse) r.entityHit;
            double speed = h.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue() * 43.17;
            double j = h.getHorseJumpStrength();
            double jump = -0.1817584952 * j * j * j + 3.689713992 * j * j + 2.128599134 * j - 0.343930367;
            List<String> l = new ArrayList<String>();
            l.add(String.format("Speed: %.2f b/s", speed));
            l.add(String.format("Jump: %.2f blocks", jump));
            l.add(String.format("Health: %.0f", h.getMaxHealth()));
            return l;
        }
    }

    static final class Momentum extends ExtraMods.Mod {
        Momentum() { super("momentum", "Momentum", ExtraMods.HUD, Items.feather, "Speed: 0.00 b/s"); }
        @Override List<String> lines(Minecraft c) {
            if (c.thePlayer == null) return null;
            double dx = c.thePlayer.posX - c.thePlayer.prevPosX, dz = c.thePlayer.posZ - c.thePlayer.prevPosZ;
            return Collections.singletonList(String.format("Speed: %.2f b/s", Math.sqrt(dx * dx + dz * dz) * 20));
        }
    }

    // --- Mob Size (render scale for non-player living entities) --------------------------

    static final class MobSize extends ExtraMods.Mod {
        MobSize() { super("mob_size", "Mob Size", ExtraMods.VISUAL, Items.spawn_egg, ""); modeLabel = "Mob scale % (100 = normal)"; modeMax = 300; ModConfig.extraMode.put(id, 100); }
        @Override public boolean hasPosition() { return false; }
        @Override public int mode() { Integer m = ModConfig.extraMode.get(id); return m == null ? 100 : Math.max(25, Math.min(300, m)); }
        @Override void draw(Minecraft c, int sw, int sh) { }
    }

    // --- Nick Hider (chat only; replaces your own name) ----------------------------------

    static final class NickHider extends ExtraMods.Mod {
        NickHider() { super("nick_hider", "Nick Hider", ExtraMods.VISUAL, Items.name_tag, ""); }
        @Override public boolean hasPosition() { return false; }
        @Override void draw(Minecraft c, int sw, int sh) { }
    }

    // --- Kill Sounds ---------------------------------------------------------------------

    static final class KillSounds extends ExtraMods.Mod {
        EntityLivingBase target; int ticks;
        KillSounds() { super("kill_sounds", "Kill Sounds", ExtraMods.UTILITY, Items.iron_sword, ""); modeLabel = "Sound (0 level up, 1 anvil, 2 orb)"; modeMax = 2; }
        @Override public boolean hasPosition() { return false; }
        @Override void draw(Minecraft c, int sw, int sh) { }
        @Override void tick(Minecraft c) {
            if (!enabled() || c.thePlayer == null) return;
            if (c.gameSettings.keyBindAttack.isKeyDown() && c.objectMouseOver != null && c.objectMouseOver.entityHit instanceof EntityLivingBase) {
                target = (EntityLivingBase) c.objectMouseOver.entityHit; ticks = 60;
            }
            if (target != null) {
                if (target.isDead || target.getHealth() <= 0) {
                    String[] snd = { "random.levelup", "random.anvil_land", "random.orb" };
                    c.thePlayer.playSound(snd[mode()], 1f, 1f);
                    target = null;
                } else if (--ticks <= 0) target = null;
            }
        }
    }

    // --- Audio Subtitles -----------------------------------------------------------------

    static final List<String> SOUNDS = new ArrayList<String>();
    static final List<Long> SOUND_TIMES = new ArrayList<Long>();

    static final class AudioSubtitles extends ExtraMods.Mod {
        AudioSubtitles() { super("audio_subtitles", "Audio Subtitles", ExtraMods.UTILITY, Items.record_13, "[step.grass]"); }
        @Override int defY(int sh) { return sh - 90; }
        @Override List<String> lines(Minecraft c) {
            long now = System.currentTimeMillis();
            List<String> l = new ArrayList<String>();
            for (int i = SOUNDS.size() - 1; i >= 0 && l.size() < 5; i--) if (now - SOUND_TIMES.get(i) < 3000) l.add("[" + SOUNDS.get(i) + "]");
            return l;
        }
    }

    // --- event hooks (registered from ExtraMods) -----------------------------------------

    @SubscribeEvent
    public void onSound(PlaySoundEvent e) {
        ExtraMods.Mod m = find("audio_subtitles");
        if (m == null || !m.enabled() || e.name == null) return;
        if (e.name.startsWith("gui.") || e.name.startsWith("music.")) return;
        if (!SOUNDS.isEmpty() && SOUNDS.get(SOUNDS.size() - 1).equals(e.name)) { SOUND_TIMES.set(SOUND_TIMES.size() - 1, System.currentTimeMillis()); return; }
        SOUNDS.add(e.name); SOUND_TIMES.add(System.currentTimeMillis());
        while (SOUNDS.size() > 20) { SOUNDS.remove(0); SOUND_TIMES.remove(0); }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        ExtraMods.Mod m = find("nick_hider");
        Minecraft c = Minecraft.getMinecraft();
        if (m == null || !m.enabled() || c.thePlayer == null || e.type == 2) return;
        String name = c.thePlayer.getName(), text = e.message.getFormattedText();
        if (text.contains(name)) e.message = new ChatComponentText(text.replace(name, "You"));
    }

    private boolean scaled;

    @SubscribeEvent
    public void onLivingPre(RenderLivingEvent.Pre e) {
        ExtraMods.Mod m = find("mob_size");
        if (m == null || !m.enabled() || e.entity instanceof EntityPlayer || m.mode() == 100) return;
        float s = m.mode() / 100f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(e.x, e.y, e.z);
        GlStateManager.scale(s, s, s);
        GlStateManager.translate(-e.x, -e.y, -e.z);
        scaled = true;
    }

    @SubscribeEvent
    public void onLivingPost(RenderLivingEvent.Post e) {
        if (scaled) { GlStateManager.popMatrix(); scaled = false; }
    }
}
