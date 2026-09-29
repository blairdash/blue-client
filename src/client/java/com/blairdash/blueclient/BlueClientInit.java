package com.blairdash.blueclient;

import com.blairdash.blueclient.features.ArmorHud;
import com.blairdash.blueclient.features.CheatWatch;
import com.blairdash.blueclient.features.CoordinatesHud;
import com.blairdash.blueclient.features.CpsFpsHud;
import com.blairdash.blueclient.features.CrosshairOverlay;
import com.blairdash.blueclient.features.Keystrokes;
import com.blairdash.blueclient.features.MotionBlurEffect;
import com.blairdash.blueclient.features.PotionHud;
import com.blairdash.blueclient.net.RankService;
import com.blairdash.blueclient.gui.BlueClientScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class BlueClientInit implements ClientModInitializer {
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("clientmod", "main"));

    // Opens/closes the mod menu overlay -- Right Shift, as requested.
    public static KeyBinding openMenuKey;
    // Held to zoom, like Lunar/OptiFine's default zoom bind.
    public static KeyBinding zoomKey;

    @Override
    public void onInitializeClient() {
        CrosshairConfigStorage.load();
        RankService.start();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.clientmod.openmenu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY));

        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.clientmod.zoom",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openMenuKey.wasPressed() && client.currentScreen == null) {
                client.setScreen(new BlueClientScreen());
            }
            CpsFpsHud.tick(client);
            CheatWatch.tick(client);
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (ModConfig.cpsFpsHudEnabled) CpsFpsHud.render(drawContext, client);
            if (ModConfig.coordinatesHudEnabled) CoordinatesHud.render(drawContext, client);
            if (ModConfig.potionHudEnabled) PotionHud.render(drawContext, client);
            if (ModConfig.armorHudEnabled) ArmorHud.render(drawContext, client);
            if (ModConfig.keystrokesEnabled) Keystrokes.render(drawContext, client);
            if (ModConfig.cheatWatchEnabled) CheatWatch.render(drawContext, client);
        });

        CrosshairOverlay.register();
        MotionBlurEffect.register();
    }
}
