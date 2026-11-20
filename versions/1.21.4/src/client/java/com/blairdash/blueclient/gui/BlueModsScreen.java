package com.blairdash.blueclient.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/** Installed Fabric mods: list on the left, details on the right. */
public class BlueModsScreen extends Screen {
    private static final int ROW = 28;
    private final Screen parent;
    private List<ModContainer> mods;
    private int selected, scroll;
    private static final Map<String, Icon> ICONS = new HashMap<>();
    private record Icon(Identifier id, int w, int h) { }

    /** Loads (once) the mod's own icon from its jar; null when it has none. */
    private Icon icon(ModContainer mod) {
        String key = mod.getMetadata().getId();
        if (ICONS.containsKey(key)) return ICONS.get(key);
        Icon result = null;
        try {
            String path = mod.getMetadata().getIconPath(64).orElse(null);
            Path p = path == null ? null : mod.findPath(path).orElse(null);
            if (p != null) {
                try (InputStream in = Files.newInputStream(p)) {
                    NativeImage img = NativeImage.read(in);
                    Identifier id = Identifier.of("clientmod", "modicon/" + key.replaceAll("[^a-z0-9_./-]", "_"));
                    client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
                    result = new Icon(id, img.getWidth(), img.getHeight());
                }
            }
        } catch (Throwable ignored) { }
        ICONS.put(key, result);
        return result;
    }

    private void drawIcon(DrawContext c, ModContainer mod, int x, int y, int size) {
        Icon ic = icon(mod);
        if (ic == null) { Ui.rrect(c, x, y, size, size, Math.max(3, size / 4), Ui.ACCENT); return; }
        c.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, ic.id(), x, y, 0f, 0f, size, size, ic.w(), ic.h(), ic.w(), ic.h());
    }

    public BlueModsScreen(Screen parent) { super(Text.literal("Mods")); this.parent = parent; }

    @Override
    protected void init() {
        mods = FabricLoader.getInstance().getAllMods().stream()
                .filter(m -> m.getOrigin().getKind() != ModOrigin.Kind.NESTED && m.getContainingMod().isEmpty() && !isBuiltIn(m.getMetadata().getId()) && !m.getMetadata().getId().equals("minecraft") && !m.getMetadata().getId().equals("java"))
                .sorted(Comparator.comparing(m -> m.getMetadata().getName().toLowerCase()))
                .collect(Collectors.toList());
    }

    private static boolean isBuiltIn(String id) {
        return (id.startsWith("fabric-") && !id.equals("fabric-api")) || id.equals("fabricloader") || id.equals("mixinextras") || id.startsWith("fabric_");
    }

    private int left() { return 24; }
    private int top() { return 56; }
    private int listW() { return Math.min(300, width / 2 - 30); }
    private int listH() { return height - top() - 60; }
    private int visible() { return Math.max(1, listH() / ROW); }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        c.fill(0, 0, width, height, 0xCC070A12);
        Ui.text(c, textRenderer, "Mods", left(), 20, Ui.TEXT);
        Ui.text(c, textRenderer, "Showing " + mods.size() + " mods", left(), 36, Ui.MUTED);

        Ui.card(c, left() - 4, top() - 4, listW() + 8, listH() + 8, 6, Ui.PANEL, Ui.PANEL_EDGE);
        for (int i = 0; i < visible() && scroll + i < mods.size(); i++) {
            int idx = scroll + i, y = top() + i * ROW;
            ModMetadata md = mods.get(idx).getMetadata();
            boolean hov = Ui.inside(mx, my, left(), y, listW(), ROW - 2);
            if (idx == selected) Ui.rrect(c, left(), y, listW(), ROW - 2, 4, Ui.SELECTED);
            else if (hov) Ui.rrect(c, left(), y, listW(), ROW - 2, 4, Ui.CARD_HOVER);
            drawIcon(c, mods.get(idx), left() + 4, y + 3, 20);
            Ui.text(c, textRenderer, Ui.clip(textRenderer, md.getName(), listW() - 48), left() + 32, y + 3, Ui.TEXT);
            Ui.text(c, textRenderer, Ui.clip(textRenderer, "v" + md.getVersion().getFriendlyString(), listW() - 48), left() + 32, y + 14, Ui.MUTED);
        }

        int dx = left() + listW() + 24, dw = width - dx - 24;
        Ui.card(c, dx, top() - 4, dw, listH() + 8, 6, Ui.PANEL, Ui.PANEL_EDGE);
        if (!mods.isEmpty()) {
            ModMetadata md = mods.get(selected).getMetadata();
            drawIcon(c, mods.get(selected), dx + 14, top() + 8, 32);
            Ui.text(c, textRenderer, md.getName(), dx + 56, top() + 8, Ui.TEXT);
            Ui.text(c, textRenderer, "v" + md.getVersion().getFriendlyString() + "  ·  " + md.getId(), dx + 56, top() + 22, Ui.MUTED);
            String authors = md.getAuthors().stream().map(Person::getName).collect(Collectors.joining(", "));
            int y = top() + 50;
            if (!authors.isEmpty()) { Ui.text(c, textRenderer, "By " + Ui.clip(textRenderer, authors, dw - 28), dx + 14, y, Ui.MUTED); y += 16; }
            for (String line : wrap(md.getDescription(), dw - 28)) {
                if (y > top() + listH() - 8) break;
                Ui.text(c, textRenderer, line, dx + 14, y, Ui.TEXT);
                y += 11;
            }
        }

        int bw = 120, bx = width / 2 - bw / 2, by = height - 36;
        boolean h = Ui.inside(mx, my, bx, by, bw, 24);
        Ui.card(c, bx, by, bw, 24, 6, h ? Ui.CARD_HOVER : Ui.CARD, h ? Ui.ACCENT : Ui.PANEL_EDGE);
        Ui.label(c, textRenderer, "Done", bx, by, bw, 24, Ui.TEXT);
    }

    private List<String> wrap(String s, int maxW) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : s.split("\\s+")) {
            String t = cur.length() == 0 ? w : cur + " " + w;
            if (Ui.width(textRenderer, t) > maxW && cur.length() > 0) { out.add(cur.toString()); cur = new StringBuilder(w); }
            else { cur = new StringBuilder(t); }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    @Override
    public boolean mouseClicked(double clickX, double clickY, int clickButton) {
        if (clickButton == 0) {
            double mx = clickX, my = clickY;
            int bw = 120, bx = width / 2 - bw / 2, by = height - 36;
            if (Ui.inside(mx, my, bx, by, bw, 24)) { close(); return true; }
            if (Ui.inside(mx, my, left(), top(), listW(), visible() * ROW)) {
                int idx = scroll + (int) ((my - top()) / ROW);
                if (idx >= 0 && idx < mods.size()) selected = idx;
                return true;
            }
        }
        return super.mouseClicked(clickX, clickY, clickButton);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll = Math.max(0, Math.min(Math.max(0, mods.size() - visible()), scroll - (int) Math.signum(v)));
        return true;
    }

    @Override
    public void close() { client.setScreen(parent); }
}
