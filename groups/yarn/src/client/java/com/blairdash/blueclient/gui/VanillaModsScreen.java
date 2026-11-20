package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** The Blue Client mod menu drawn with plain Minecraft widgets (used when "Use Minecraft GUI" is on). */
public class VanillaModsScreen extends Screen {
    private static final int PER_PAGE = 12; // 2 columns x 6 rows

    private final Screen parent;
    private final List<ModEntry> mods = new ArrayList<>();
    private int page;

    public VanillaModsScreen(Screen parent) {
        super(Text.literal("Blue Client Mods"));
        this.parent = parent;
    }

    private static Text label(ModEntry m) {
        return Text.literal(m.name + ": " + (m.enabled.get() ? "ON" : "OFF"));
    }

    private static void go(Screen s) { MinecraftClient.getInstance().setScreen(s); }

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
            addDrawableChild(ButtonWidget.builder(label(m), b -> {
                m.setEnabled.accept(!m.enabled.get());
                CrosshairConfigStorage.save();
                b.setMessage(label(m));
            }).dimensions(x, y, hasOptions ? 128 : 150, 20).build());
            if (hasOptions) {
                addDrawableChild(ButtonWidget.builder(Text.literal("..."), b -> {
                    CrosshairConfigStorage.save();
                    go(m.options.apply(this));
                }).dimensions(x + 130, y, 20, 20).build());
            }
        }
        int by = 36 + 6 * 24 + 8;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { if (page > 0) { page--; rebuild(); } }).dimensions(cx - 154, by, 30, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Edit HUD Layout"), b -> { CrosshairConfigStorage.save(); go(new HudLayoutScreen(this)); }).dimensions(cx - 120, by, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Blue Client Look"), b -> {
            BlueMenuConfig.setBlueMenu(true);
            CrosshairConfigStorage.save();
            go(new BlueClientScreen());
        }).dimensions(cx + 2, by, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { if (page < pages() - 1) { page++; rebuild(); } }).dimensions(cx + 124, by, 30, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(cx - 100, by + 24, 200, 20).build());
    }

    private void rebuild() { clearChildren(); init(); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, "Blue Client Mods  (" + (page + 1) + "/" + pages() + ")", width / 2, 14, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        CrosshairConfigStorage.save();
        go(parent);
    }

    @Override
    public boolean shouldPause() { return false; }
}
