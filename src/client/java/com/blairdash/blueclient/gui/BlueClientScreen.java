package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.CrosshairConfigStorage;
import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class BlueClientScreen extends Screen {
    private enum Tab { MODS, COSMETICS }
    private Tab activeTab = Tab.MODS;

    private static final int COL_WIDTH = 200;
    private static final int COL_GAP = 8;
    private static final int START_Y = 55;
    private static final int ROW_HEIGHT = 24;

    private ButtonWidget crosshairButton;

    public BlueClientScreen() {
        super(Text.literal("Blue Client"));
    }

    @Override
    protected void init() {
        int tabY = 20;
        addDrawableChild(ButtonWidget.builder(Text.literal("Mods"), b -> { activeTab = Tab.MODS; rebuild(); })
                .dimensions(width / 2 - 105, tabY, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cosmetics"), b -> { activeTab = Tab.COSMETICS; rebuild(); })
                .dimensions(width / 2 + 5, tabY, 100, 20).build());

        if (activeTab == Tab.MODS) buildModsTab();
        else buildCosmeticsTab();
    }

    private void rebuild() {
        clearChildren();
        init();
    }

    private int slotX(int index) {
        return (index % 2 == 0)
                ? width / 2 - COL_WIDTH - COL_GAP / 2
                : width / 2 + COL_GAP / 2;
    }

    private int slotY(int index) {
        return START_Y + (index / 2) * ROW_HEIGHT;
    }

    private void buildModsTab() {
        addToggle(0, "Zoom (hold C)", ModConfig.zoomEnabled, v -> ModConfig.zoomEnabled = v);
        crosshairButton = addToggle(1, "Crosshair (right-click)", ModConfig.customCrosshairEnabled, v -> ModConfig.customCrosshairEnabled = v);
        addToggle(2, "CPS / FPS Display", ModConfig.cpsFpsHudEnabled, v -> ModConfig.cpsFpsHudEnabled = v);
        addToggle(3, "Coordinates", ModConfig.coordinatesHudEnabled, v -> ModConfig.coordinatesHudEnabled = v);
        addToggle(4, "Motion Blur", ModConfig.motionBlurEnabled, v -> ModConfig.motionBlurEnabled = v);
        addToggle(5, "Self Nametag (F5)", ModConfig.selfNametagEnabled, v -> ModConfig.selfNametagEnabled = v);
        addToggle(6, "Potion HUD", ModConfig.potionHudEnabled, v -> ModConfig.potionHudEnabled = v);
        addToggle(7, "Armor HUD", ModConfig.armorHudEnabled, v -> ModConfig.armorHudEnabled = v);
        addToggle(8, "Low Fire", ModConfig.lowFireEnabled, v -> ModConfig.lowFireEnabled = v);
        addToggle(9, "Low Shield", ModConfig.lowShieldEnabled, v -> ModConfig.lowShieldEnabled = v);
        addToggle(10, "Cheat Watch (risky)", ModConfig.cheatWatchEnabled, v -> ModConfig.cheatWatchEnabled = v);
        addToggle(11, "Keystrokes", ModConfig.keystrokesEnabled, v -> ModConfig.keystrokesEnabled = v);

        addDrawableChild(ButtonWidget.builder(Text.literal("HUD Positions..."),
                        b -> MinecraftClient.getInstance().setScreen(new HudPositionScreen(this)))
                .dimensions(slotX(12), slotY(12), COL_WIDTH, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Fire / Shield Offsets..."),
                        b -> MinecraftClient.getInstance().setScreen(new ViewOffsetScreen(this)))
                .dimensions(slotX(13), slotY(13), COL_WIDTH, 20).build());
    }

    private void buildCosmeticsTab() {
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Cosmetics need a backend (see README) -- none loaded"),
                b -> {}).dimensions(width / 2 - 150, 60, 300, 20).build());
    }

    private ButtonWidget addToggle(int slot, String label, boolean initial, Consumer<Boolean> onChange) {
        boolean[] state = { initial };
        ButtonWidget button = ButtonWidget.builder(toggleText(label, state[0]), b -> {
            state[0] = !state[0];
            onChange.accept(state[0]);
            b.setMessage(toggleText(label, state[0]));
            CrosshairConfigStorage.save();
        }).dimensions(slotX(slot), slotY(slot), COL_WIDTH, 20).build();
        addDrawableChild(button);
        return button;
    }

    private Text toggleText(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "ON" : "OFF"));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button == 1 && crosshairButton != null && activeTab == Tab.MODS) {
            int bx = crosshairButton.getX();
            int by = crosshairButton.getY();
            int bw = crosshairButton.getWidth();
            int bh = crosshairButton.getHeight();
            if (mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh) {
                MinecraftClient.getInstance().setScreen(new CrosshairSettingsScreen(this));
                return true;
            }
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 6, 0xFFFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}