package com.blairdash.blueclient;

import com.blairdash.blueclient.features.ArmorHud;
import com.blairdash.blueclient.features.CheatWatch;
import com.blairdash.blueclient.features.CoordinatesHud;
import com.blairdash.blueclient.features.CpsFpsHud;
import com.blairdash.blueclient.features.CrosshairOverlay;
import com.blairdash.blueclient.features.Freelook;
import com.blairdash.blueclient.features.InfoHuds;
import com.blairdash.blueclient.features.Keystrokes;
import com.blairdash.blueclient.features.PvpStats;
import com.blairdash.blueclient.features.ToggleMovement;
import com.blairdash.blueclient.features.WorldTweaks;
import com.blairdash.blueclient.features.MotionBlurEffect;
import com.blairdash.blueclient.features.ZoomFeature;
import com.blairdash.blueclient.features.PotionHud;
import com.blairdash.blueclient.net.RankService;
import com.blairdash.blueclient.mods.ModManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.blairdash.blueclient.gui.BlueClientScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class BlueClientInit implements ClientModInitializer {
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("clientmod", "main"));

    // Opens/closes the mod menu overlay -- Right Shift, as requested.
    public static KeyMapping openMenuKey;
    // Held to zoom, like Lunar/OptiFine's default zoom bind.
    public static KeyMapping zoomKey;
    // Held to look around without turning the player.
    public static KeyMapping freelookKey;

    @Override
    public void onInitializeClient() {
        com.blairdash.blueclient.LauncherUpdater.start();
        com.blairdash.blueclient.gui.VanillaMenuButton.register();
        ModManager.init();
        CrosshairConfigStorage.load();
        RankService.start();

        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.clientmod.openmenu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY));

        zoomKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.clientmod.zoom",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                CATEGORY));

        freelookKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.clientmod.freelook",
                InputConstants.Type.KEYSYM,
                342,
                CATEGORY));

        PvpStats.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            migrateFreelookKey(client);
            autoBindKeys(client);
            if (openMenuKey.consumeClick() && client.gui.screen() == null) {
                client.gui.setScreen(com.blairdash.blueclient.gui.BlueMenuConfig.blueMenu() ? new BlueClientScreen() : new com.blairdash.blueclient.gui.VanillaModsScreen(null));
            }
            CpsFpsHud.tick(client);
            CheatWatch.tick(client);
            ZoomFeature.tick(client);
            ToggleMovement.tick(client);
            Freelook.tick(client);
            WorldTweaks.tick(client);
            PvpStats.tick(client);
            ModManager.tick(client);
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("clientmod", "hud"),
                (drawContext, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (ModConfig.cpsFpsHudEnabled) CpsFpsHud.render(drawContext, client);
            if (ModConfig.coordinatesHudEnabled) CoordinatesHud.render(drawContext, client);
            if (ModConfig.potionHudEnabled) PotionHud.render(drawContext, client);
            if (ModConfig.armorHudEnabled) ArmorHud.render(drawContext, client);
            if (ModConfig.keystrokesEnabled) Keystrokes.render(drawContext, client);
            if (ModConfig.cheatWatchEnabled) CheatWatch.render(drawContext, client);
            InfoHuds.render(drawContext, client);
            ModManager.renderHud(drawContext, client);
        });

        CrosshairOverlay.register();
        MotionBlurEffect.register();
    }

    private static boolean autoBound;
    /** Gives every unbound Blue Client key a default (once per key), so nothing has to be bound by hand. */
    private static void autoBindKeys(Minecraft client) {
        if (autoBound || client.options == null) return;
        autoBound = true;
        try {
            String base = System.getenv("LOCALAPPDATA");
            java.nio.file.Path dir = base != null ? java.nio.file.Path.of(base, "BlueClient") : java.nio.file.Path.of(System.getProperty("user.home"), ".blueclient");
            java.nio.file.Path doneFile = dir.resolve("autobind.txt");
            java.util.Set<String> handled = new java.util.HashSet<>();
            if (java.nio.file.Files.exists(doneFile)) handled.addAll(java.nio.file.Files.readAllLines(doneFile));
            java.util.Set<String> used = new java.util.HashSet<>();
            for (KeyMapping m : client.options.keyMappings) if (!m.isUnbound()) used.add(m.saveString());
            java.util.Map<String, Integer> pref = java.util.Map.of(
                    "key.clientmod.waypoint_add", 66, "key.clientmod.waypoint_remove", 78, "key.clientmod.waypoint_menu", 75);
            int[] pool = { 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 295, 296, 297, 298, 299, 301, 268, 269, 260, 266, 267 };
            boolean changed = false;
            for (KeyMapping m : client.options.keyMappings) {
                String n = m.getName();
                if (!n.startsWith("key.clientmod.") || !handled.add(n)) continue;
                if (!m.isUnbound()) continue;
                Integer p = pref.get(n);
                if (p != null) {
                    InputConstants.Key k = InputConstants.Type.KEYSYM.getOrCreate(p);
                    if (used.add(k.getName())) { m.setKey(k); changed = true; continue; }
                }
                for (int code : pool) {
                    InputConstants.Key k = InputConstants.Type.KEYSYM.getOrCreate(code);
                    if (used.add(k.getName())) { m.setKey(k); changed = true; break; }
                }
            }
            if (changed) { KeyMapping.resetMapping(); client.options.save(); }
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.write(doneFile, handled);
        } catch (Throwable t) { System.err.println("[clientmod] auto bind: " + t); }
    }
    /** One-time: players who still have the old default (V) get Freelook moved to Left Alt. */
    private static boolean freelookMigrated;
    private static void migrateFreelookKey(Minecraft client) {
        if (freelookMigrated || freelookKey == null || client.options == null) return;
        freelookMigrated = true;
        try {
            String base = System.getenv("LOCALAPPDATA");
            java.nio.file.Path dir = base != null ? java.nio.file.Path.of(base, "BlueClient") : java.nio.file.Path.of(System.getProperty("user.home"), ".blueclient");
            java.nio.file.Path flag = dir.resolve("freelook-leftalt.flag");
            if (java.nio.file.Files.exists(flag)) return;
            if ("key.keyboard.v".equals(freelookKey.saveString())) {
                freelookKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(342));
                KeyMapping.resetMapping();
                client.options.save();
            }
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(flag, "1");
        } catch (Throwable t) { System.err.println("[clientmod] freelook key migration: " + t); }
    }
}