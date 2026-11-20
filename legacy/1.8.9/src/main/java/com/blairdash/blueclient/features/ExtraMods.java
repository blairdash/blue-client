package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ports of the extra Blue Client mods from the 1.21.x/26.x builds (Item Counter, Potion Counter,
 * Day Counter, Stopwatch, Playtime, Server Address, Saturation, Inventory HUD, WAILA, F3 Display,
 * Pack Display, Damage Tint). Enabled state, position and the one "mode" slider are kept in
 * {@link ModConfig} maps so they are saved with the rest of the settings.
 */
public class ExtraMods {
    public static final String HUD = "HUD";
    public static final String VISUAL = "VISUAL";
    public static final String UTILITY = "UTILITY";

    /** One extra mod. HUD mods return text lines; others override {@link #draw}. */
    public abstract static class Mod {
        public final String id, name, category, sample;
        public final ItemStack icon;
        /** Name of the mode slider (null = no options); 0..modeMax. */
        public String modeLabel;
        public int modeMax;
        /** Optional extra sliders (index i+1), stored under id#(i+1). */
        public String[] moreLabels = new String[0];
        public int[] moreMax = new int[0];
        public int more(int i) { Integer m = ModConfig.extraMode.get(id + "#" + i); return m == null ? 0 : Math.max(0, Math.min(moreMax[i - 1], m)); }
        public void setMore(int i, int v) { ModConfig.extraMode.put(id + "#" + i, v); }

        Mod(String id, String name, String category, Item icon, String sample) {
            this.id = id; this.name = name; this.category = category; this.sample = sample;
            this.icon = new ItemStack(icon);
        }

        public boolean enabled() { Boolean b = ModConfig.extraEnabled.get(id); return b != null && b; }
        public void setEnabled(boolean v) { ModConfig.extraEnabled.put(id, v); }
        public int mode() { Integer m = ModConfig.extraMode.get(id); return m == null ? 0 : Math.max(0, Math.min(modeMax, m)); }
        public void setMode(int v) { ModConfig.extraMode.put(id, v); }
        public boolean hasPosition() { return true; }

        public int x(int sw, Minecraft c) { Integer v = ModConfig.extraX.get(id); return v != null && v >= 0 ? v : defX(sw, c); }
        public int y(int sh) { Integer v = ModConfig.extraY.get(id); return v != null && v >= 0 ? v : defY(sh); }
        public void setPos(int x, int y) { ModConfig.extraX.put(id, x); ModConfig.extraY.put(id, y); }
        public int w(Minecraft c) { return Math.max(40, c.fontRendererObj.getStringWidth(sample)); }
        public int h() { return 10 * Math.max(1, sample.split("\n").length); }

        int defX(int sw, Minecraft c) { return 6; }
        int defY(int sh) { return 70 + 12 * index(this); }

        void tick(Minecraft c) { }
        /** Text lines for simple HUDs; null/empty = draw nothing. */
        List<String> lines(Minecraft c) { return null; }
        /** Custom drawing; default draws {@link #lines}. */
        void draw(Minecraft c, int sw, int sh) {
            List<String> l = lines(c);
            if (l == null) return;
            int x = x(sw, c), y = y(sh);
            for (int i = 0; i < l.size(); i++) c.fontRendererObj.drawStringWithShadow(l.get(i), x, y + i * 10, 0xFFFFFFFF);
        }
    }

    public static final List<Mod> ALL = new ArrayList<Mod>();
    private static int index(Mod m) { return ALL.indexOf(m); }

    static int count(Minecraft c, Item item, int meta) {
        int n = 0;
        for (ItemStack s : c.thePlayer.inventory.mainInventory) {
            if (s != null && s.getItem() == item && (meta < 0 || s.getMetadata() == meta)) n += s.stackSize;
        }
        return n;
    }

    static String time(long seconds) {
        long h = seconds / 3600, m = (seconds / 60) % 60, s = seconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }

    // --- Item Counter ------------------------------------------------------------------

    private static final String[] ITEM_NAMES = { "Held item", "Golden apples", "Ender pearls", "Arrows", "Obsidian", "Cobwebs", "Splash health" };

    static final class ItemCounter extends Mod {
        ItemCounter() { super("item_counter", "Item Counter", HUD, Items.chest_minecart, "Golden apples: 000"); modeLabel = "Count (0 held, 1 gapples, 2 pearls, 3 arrows, 4 obsidian, 5 cobwebs, 6 health pots)"; modeMax = 6; }
        @Override List<String> lines(Minecraft c) {
            if (c.thePlayer == null) return null;
            Item it;
            switch (mode()) {
                case 1: it = Items.golden_apple; break;
                case 2: it = Items.ender_pearl; break;
                case 3: it = Items.arrow; break;
                case 4: it = Item.getItemFromBlock(Blocks.obsidian); break;
                case 5: it = Item.getItemFromBlock(Blocks.web); break;
                case 6: return Collections.singletonList("Health pots: " + healthPots(c));
                default:
                    ItemStack held = c.thePlayer.getHeldItem();
                    if (held == null) return null;
                    return Collections.singletonList(held.getDisplayName() + ": " + count(c, held.getItem(), held.getHasSubtypes() ? held.getMetadata() : -1));
            }
            return Collections.singletonList(ITEM_NAMES[mode()] + ": " + count(c, it, -1));
        }
    }

    static int healthPots(Minecraft c) {
        int n = 0;
        for (ItemStack s : c.thePlayer.inventory.mainInventory) {
            if (s == null || s.getItem() != Items.potionitem || !ItemPotion.isSplash(s.getMetadata())) continue;
            List<PotionEffect> fx = Items.potionitem.getEffects(s);
            if (fx == null) continue;
            for (PotionEffect e : fx) if (e.getPotionID() == Potion.heal.id) { n += s.stackSize; break; }
        }
        return n;
    }

    // --- Potion Counter ----------------------------------------------------------------

    static final class PotionCounter extends Mod {
        PotionCounter() { super("potion_counter", "Potion Counter", HUD, Items.potionitem, "Health pots: 00"); }
        @Override List<String> lines(Minecraft c) {
            return c.thePlayer == null ? null : Collections.singletonList("Health pots: " + healthPots(c));
        }
    }

    // --- Day Counter -------------------------------------------------------------------

    static final class DayCounter extends Mod {
        DayCounter() { super("day_counter", "Day Counter", HUD, Items.clock, "Day 0000"); }
        @Override List<String> lines(Minecraft c) {
            return c.theWorld == null ? null : Collections.singletonList("Day " + (c.theWorld.getWorldTime() / 24000L + 1));
        }
    }

    // --- Stopwatch (key: unbound by default; bind it in Controls) -----------------------

    static final class Stopwatch extends Mod {
        long accumulated; long startedAt = -1;
        Stopwatch() { super("stopwatch", "Stopwatch", HUD, Items.clock, "0:00.0"); }
        long elapsed() { return accumulated + (startedAt < 0 ? 0 : System.currentTimeMillis() - startedAt); }
        @Override void tick(Minecraft c) {
            if (c.currentScreen != null) return;
            while (startStop.isPressed()) { if (startedAt < 0) startedAt = System.currentTimeMillis(); else { accumulated = elapsed(); startedAt = -1; } }
            while (reset.isPressed()) { accumulated = 0; startedAt = startedAt < 0 ? -1 : System.currentTimeMillis(); }
        }
        @Override List<String> lines(Minecraft c) {
            long ms = elapsed();
            return Collections.singletonList(time(ms / 1000) + "." + (ms / 100) % 10);
        }
    }

    // --- Playtime / Server address -----------------------------------------------------

    static final long LAUNCHED = System.currentTimeMillis();

    static final class Playtime extends Mod {
        Playtime() { super("playtime", "Playtime", HUD, Items.clock, "Playtime: 0:00:00"); }
        @Override List<String> lines(Minecraft c) {
            return Collections.singletonList("Playtime: " + time((System.currentTimeMillis() - LAUNCHED) / 1000));
        }
    }

    static final class ServerAddress extends Mod {
        ServerAddress() { super("server_address", "Server Address", HUD, Items.compass, "play.example.com"); }
        @Override List<String> lines(Minecraft c) {
            return Collections.singletonList(c.getCurrentServerData() == null ? "Singleplayer" : c.getCurrentServerData().serverIP);
        }
    }

    // --- Saturation --------------------------------------------------------------------

    static final class Saturation extends Mod {
        Saturation() { super("saturation", "Saturation", HUD, Items.cooked_beef, "Sat: 20.0"); }
        @Override List<String> lines(Minecraft c) {
            if (c.thePlayer == null || c.thePlayer.capabilities.isCreativeMode) return null;
            return Collections.singletonList(String.format("Sat: %.1f", c.thePlayer.getFoodStats().getSaturationLevel()));
        }
    }

    // --- WAILA -------------------------------------------------------------------------

    static final class Waila extends Mod {
        Waila() { super("waila", "WAILA", HUD, Items.spider_eye, "Cobblestone"); }
        @Override int defX(int sw, Minecraft c) { return sw / 2 - 40; }
        @Override int defY(int sh) { return 4; }
        @Override List<String> lines(Minecraft c) {
            MovingObjectPosition r = c.objectMouseOver;
            if (r == null || c.theWorld == null) return null;
            if (r.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && r.entityHit != null)
                return Collections.singletonList(r.entityHit.getName());
            if (r.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                Block b = c.theWorld.getBlockState(r.getBlockPos()).getBlock();
                return Collections.singletonList(b.getLocalizedName());
            }
            return null;
        }
    }

    // --- F3 Display (mini) -------------------------------------------------------------

    static final class MiniF3 extends Mod {
        MiniF3() { super("mini_f3", "F3 Display", HUD, Items.map, "XYZ: 0.0 / 0.0 / 0.0\nFacing: north\nBiome: Plains"); }
        @Override List<String> lines(Minecraft c) {
            if (c.thePlayer == null || c.theWorld == null) return null;
            BlockPos p = new BlockPos(c.thePlayer);
            List<String> l = new ArrayList<String>();
            l.add(String.format("XYZ: %.1f / %.1f / %.1f", c.thePlayer.posX, c.thePlayer.posY, c.thePlayer.posZ));
            l.add("Facing: " + c.thePlayer.getHorizontalFacing().getName());
            l.add("Biome: " + c.theWorld.getBiomeGenForCoords(p).biomeName);
            return l;
        }
    }

    // --- Pack Display ------------------------------------------------------------------

    static final class PackDisplay extends Mod {
        PackDisplay() { super("pack_display", "Pack Display", HUD, Items.painting, "Resource packs:\nDefault"); }
        @Override List<String> lines(Minecraft c) {
            List<String> l = new ArrayList<String>();
            @SuppressWarnings("unchecked")
            List<ResourcePackRepository.Entry> es = c.getResourcePackRepository().getRepositoryEntries();
            for (ResourcePackRepository.Entry e : es) l.add(e.getResourcePackName());
            if (l.isEmpty()) l.add("Default");
            l.add(0, "Resource packs:");
            return l;
        }
    }

    // --- Inventory HUD -----------------------------------------------------------------

    static final class InventoryHud extends Mod {
        InventoryHud() { super("inventory_hud", "Inventory HUD", HUD, Item.getItemFromBlock(Blocks.chest), "[inventory]"); }
        @Override public int w(Minecraft c) { return 9 * 18 + 4; }
        @Override public int h() { return 3 * 18 + 4; }
        @Override int defY(int sh) { return sh - 3 * 18 - 40; }
        @Override void draw(Minecraft c, int sw, int sh) {
            if (c.thePlayer == null) return;
            int x = x(sw, c), y = y(sh);
            Gui.drawRect(x, y, x + w(c), y + h(), 0x66000000);
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableGUIStandardItemLighting();
            for (int i = 0; i < 27; i++) {
                ItemStack s = c.thePlayer.inventory.mainInventory[9 + i];
                if (s == null) continue;
                int ix = x + 2 + (i % 9) * 18, iy = y + 2 + (i / 9) * 18;
                c.getRenderItem().renderItemAndEffectIntoGUI(s, ix, iy);
                c.getRenderItem().renderItemOverlays(c.fontRendererObj, s, ix, iy);
            }
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableLighting();
        }
    }

    // --- Damage Tint (full-screen overlay, no HUD box) ---------------------------------

    static final class DamageTint extends Mod {
        DamageTint() { super("damage_tint", "Damage Tint", VISUAL, Items.redstone, ""); modeLabel = "Strength (0 light, 1 medium, 2 strong)"; modeMax = 2; }
        @Override public boolean hasPosition() { return false; }
        @Override void draw(Minecraft c, int sw, int sh) {
            if (c.thePlayer == null || c.thePlayer.hurtTime <= 0) return;
            float t = c.thePlayer.hurtTime / (float) Math.max(1, c.thePlayer.maxHurtTime);
            int a = (int) (t * (60 + mode() * 50));
            Gui.drawRect(0, 0, sw, sh, (Math.min(255, a) << 24) | 0xAA0000);
        }
    }

    // --- Cosmetics (Blue Client cloak, shown on your own player) --------------------------

    public static final class Cosmetics extends Mod {
        Cosmetics() { super("cosmetics", "Cosmetics", "COSMETICS", Items.banner, ""); modeLabel = "Cloak (0 none, 1 light, 2 dark, 3 blue)"; modeMax = 3; }
        @Override public boolean hasPosition() { return false; }
        @Override public int mode() { Integer m = ModConfig.extraMode.get(id); return m == null ? 1 : Math.max(0, Math.min(modeMax, m)); }
    }

    /** The enabled Cosmetics mod, or null. */
    public static Mod cosmetics() {
        for (Mod m : ALL) if (m instanceof Cosmetics) return m.enabled() ? m : null;
        return null;
    }

    // --- FOV Changer (multiplies the FOV the game computes; 100 = unchanged) --------------

    static final class FovChanger extends Mod {
        FovChanger() { super("fov_changer", "FOV Changer", VISUAL, Items.ender_eye, ""); modeLabel = "FOV scale % (100 = normal)"; modeMax = 150; ModConfig.extraMode.put(id, 100); }
        @Override public boolean hasPosition() { return false; }
        @Override public int mode() { Integer m = ModConfig.extraMode.get(id); return m == null ? 100 : Math.max(20, Math.min(modeMax, m)); }
    }

    @SubscribeEvent
    public void onFov(net.minecraftforge.client.event.FOVUpdateEvent e) {
        for (Mod m : ALL) if (m instanceof FovChanger && m.enabled()) e.newfov *= m.mode() / 100f;
    }

    // ------------------------------------------------------------------------------------

    static net.minecraft.client.settings.KeyBinding startStop, reset;

    static {
        ALL.add(new ItemCounter());
        ALL.add(new PotionCounter());
        ALL.add(new DayCounter());
        ALL.add(new Stopwatch());
        ALL.add(new Playtime());
        ALL.add(new ServerAddress());
        ALL.add(new Saturation());
        ALL.add(new Waila());
        ALL.add(new MiniF3());
        ALL.add(new PackDisplay());
        ALL.add(new InventoryHud());
        ALL.add(new DamageTint());
        ALL.add(new FovChanger());
        ALL.add(new Cosmetics());
        MoreMods.register();
    }

    public ExtraMods() {
        startStop = new net.minecraft.client.settings.KeyBinding("key.blueclient.stopwatch", Keyboard.KEY_NONE, "key.categories.blueclient");
        reset = new net.minecraft.client.settings.KeyBinding("key.blueclient.stopwatch_reset", Keyboard.KEY_NONE, "key.categories.blueclient");
        ClientRegistry.registerKeyBinding(startStop);
        ClientRegistry.registerKeyBinding(reset);
        MoreMods.registerKeys();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new MoreMods());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft c = Minecraft.getMinecraft();
        for (Mod m : ALL) m.tick(c);
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft c = Minecraft.getMinecraft();
        if (c.thePlayer == null || c.gameSettings.hideGUI) return;
        int sw = e.resolution.getScaledWidth(), sh = e.resolution.getScaledHeight();
        for (Mod m : ALL) {
            if (!m.enabled()) continue;
            m.draw(c, sw, sh);
        }
    }
}
