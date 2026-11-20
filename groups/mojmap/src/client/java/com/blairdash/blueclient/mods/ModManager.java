package com.blairdash.blueclient.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Holds every {@link BlueMod}, saves/loads their state, and drives ticking and HUD drawing. */
public final class ModManager {
    private ModManager() { }

    private static final List<BlueMod> ALL = new ArrayList<>();
    private static final Map<String, BlueMod> BY_ID = new LinkedHashMap<>();
    private static boolean initialised;

    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        for (java.util.function.Consumer<List<BlueMod>> group : ModList.groups()) {
            List<BlueMod> made = new ArrayList<>();
            try { group.accept(made); } catch (Throwable t) { System.err.println("[clientmod] mod group failed: " + t); }
            for (BlueMod m : made) { ALL.add(m); BY_ID.put(m.id, m); }
        }
    }

    public static List<BlueMod> all() { init(); return ALL; }
    public static BlueMod get(String id) { init(); return BY_ID.get(id); }
    /** Enabled state of a mod, false if it doesn't exist (used from mixins). */
    public static boolean on(String id) { BlueMod m = get(id); return m != null && m.enabled; }

    public static void tick(Minecraft c) {
        if (c.player == null || c.level == null) {
            for (BlueMod m : all()) { try { m.tickMenu(c); } catch (Throwable t) { warnOnce(m, t); } }
            return;
        }
        for (BlueMod m : all()) {
            try { m.tick(c); } catch (Throwable t) { warnOnce(m, t); }
        }
    }

    public static void renderHud(GuiGraphicsExtractor ctx, Minecraft c) {
        if (c.player == null) return;
        for (BlueMod m : all()) {
            if (!m.enabled || !(m.isHud() || m.drawsOverlay())) continue;
            try { m.render(ctx, c); } catch (Throwable t) { warnOnce(m, t); }
        }
    }

    private static final java.util.Set<String> WARNED = new java.util.HashSet<>();
    private static void warnOnce(BlueMod m, Throwable t) {
        if (WARNED.add(m.id)) { System.err.println("[clientmod] " + m.name + " failed: " + t); t.printStackTrace(); }
    }

    // --- saving ------------------------------------------------------------
    public static Map<String, Map<String, Object>> export() {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (BlueMod m : all()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("enabled", m.enabled);
            if (m.id.equals("saturation") || m.id.equals("waypoints")) o.put("default_on_v2", true);
            if (m.isHud()) { o.put("x", m.hudX); o.put("y", m.hudY); }
            for (Setting s : m.settings) o.put("s." + s.id, s.export());
            out.put(m.id, o);
        }
        return out;
    }

    public static void apply(Map<String, Map<String, Object>> data) {
        if (data == null) return;
        for (BlueMod m : all()) {
            Map<String, Object> o = data.get(m.id);
            if (o == null) continue;
            Object en = o.get("enabled");
            if (en instanceof Boolean) m.enabled = (Boolean) en;
            if ((m.id.equals("saturation") || m.id.equals("waypoints")) && !Boolean.TRUE.equals(o.get("default_on_v2"))) m.enabled = true;
            if (o.get("x") instanceof Number) m.hudX = ((Number) o.get("x")).intValue();
            if (o.get("y") instanceof Number) m.hudY = ((Number) o.get("y")).intValue();
            for (Setting s : m.settings) s.importValue(o.get("s." + s.id));
        }
    }

    public static void resetHudPositions() {
        for (BlueMod m : all()) { m.hudX = -1; m.hudY = -1; }
    }
}
