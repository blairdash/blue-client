package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** The Blue Client mod menu drawn with plain Minecraft widgets (used when "Use Minecraft GUI" is on). */
public class VanillaModsScreen extends Screen {
    private static final int PER_PAGE = 12; // 2 columns x 6 rows

    private final Screen parent;
    private final List<ModEntry> mods = new ArrayList<>();
    private int page;

    public VanillaModsScreen(Screen parent) {
        super(Component.literal("Blue Client Mods"));
        this.parent = parent;
    }

    private static Component label(ModEntry m) {
        return Component.literal(m.name + ": " + (m.enabled.get() ? "ON" : "OFF"));
    }

    private static void go(Screen s) { ScreenNav.open(s); }

    private int pages() { return Math.max(1, (mods.size() + PER_PAGE - 1) / PER_PAGE); }

    @Override
    protected void init() {
        if (mods.isEmpty()) mods.addAll(ModRegistry.create());
        if (page >= pages()) page = pages() - 1;
        int cx = width / 2;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= mods.size()) break;
            ModEntry m = mods.get(idx);
            int x = cx - 154 + (i % 2) * 158, y = 36 + (i / 2) * 24;
            boolean hasOptions = m.options != null;
            addRenderableWidget(Button.builder(label(m), b -> {
                m.setEnabled.accept(!m.enabled.get());
                CrosshairConfigStorage.save();
                b.setMessage(label(m));
            }).bounds(x, y, hasOptions ? 128 : 150, 20).build());
            if (hasOptions) {
                addRenderableWidget(Button.builder(Component.literal("..."), b -> {
                    CrosshairConfigStorage.save();
                    go(m.options.apply(this));
                }).bounds(x + 130, y, 20, 20).build());
            }
        }
        int by = 36 + 6 * 24 + 8;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { if (page > 0) { page--; rebuild(); } }).bounds(cx - 154, by, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Edit HUD Layout"), b -> { CrosshairConfigStorage.save(); go(new HudLayoutScreen(this)); }).bounds(cx - 120, by, 118, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Blue Client Look"), b -> {
            BlueMenuConfig.setBlueMenu(true);
            CrosshairConfigStorage.save();
            go(new BlueClientScreen());
        }).bounds(cx + 2, by, 118, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { if (page < pages() - 1) { page++; rebuild(); } }).bounds(cx + 124, by, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(cx - 100, by + 24, 200, 20).build());
    }

    private void rebuild() { clearWidgets(); init(); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx) {
        super.extractRenderState(ctx);
        ctx.centeredText(font, "Blue Client Mods  (" + (page + 1) + "/" + pages() + ")", width / 2, 14, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        CrosshairConfigStorage.save();
        go(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
