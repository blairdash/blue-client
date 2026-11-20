package com.blairdash.blueclient.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * A mod card in the Blue Client menu. Subclasses add settings with the helper methods, react in
 * {@link #tick} and (for HUD mods) draw in {@link #render}. Enabled state, settings and HUD position
 * are saved with the profile automatically by {@link ModManager}.
 */
public abstract class BlueMod {
    public final String id, name, category;
    public final Item icon;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    // HUD position in scaled GUI pixels; -1 = use the default spot.
    public int hudX = -1, hudY = -1;

    protected BlueMod(String id, String name, String category, Item icon) {
        this.id = id; this.name = name; this.category = category; this.icon = icon;
    }

    // --- settings helpers ------------------------------------------------
    protected Setting bool(String id, String label, boolean def) { Setting s = Setting.bool(id, label, def); settings.add(s); return s; }
    protected Setting num(String id, String label, double min, double max, int dec, double def) { Setting s = Setting.num(id, label, min, max, dec, def); settings.add(s); return s; }
    protected Setting choice(String id, String label, int def, String... o) { Setting s = Setting.choice(id, label, def, o); settings.add(s); return s; }
    protected Setting color(String id, String label, int def) { Setting s = Setting.color(id, label, def); settings.add(s); return s; }

    // --- behaviour -------------------------------------------------------
    /** Called every client tick while in a world, whether or not the mod is enabled. */
    public void tick(Minecraft c) { }

    /** Called every client tick while NOT in a world (title screen, menus). */
    public void tickMenu(Minecraft c) { }

    /** True for mods that draw on the HUD (they get a box in the HUD layout editor). */
    public boolean isHud() { return false; }

    /** True for mods that draw a full-screen overlay without a movable box (e.g. damage tint). */
    public boolean drawsOverlay() { return false; }

    /** Draws the HUD element. Only called when enabled and in a world. */
    public void render(GuiGraphicsExtractor ctx, Minecraft c) { }

    public int defaultX(Minecraft c, int sw, int sh) { return 6; }
    public int defaultY(Minecraft c, int sw, int sh) { return 70; }
    public int hudW(Minecraft c) { return 80; }
    public int hudH(Minecraft c) { return 10; }

    public final int x(Minecraft c, int sw, int sh) { return hudX >= 0 ? hudX : defaultX(c, sw, sh); }
    public final int y(Minecraft c, int sw, int sh) { return hudY >= 0 ? hudY : defaultY(c, sw, sh); }

    public Setting setting(String id) {
        for (Setting s : settings) if (s.id.equals(id)) return s;
        throw new IllegalArgumentException("no setting " + id + " on " + this.id);
    }
}
