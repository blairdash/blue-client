package com.blairdash.blueclient;

import com.blairdash.blueclient.features.ArmorHud;
import com.blairdash.blueclient.features.CheatWatch;
import com.blairdash.blueclient.features.CoordinatesHud;
import com.blairdash.blueclient.features.CpsFpsHud;
import com.blairdash.blueclient.features.Freelook;
import com.blairdash.blueclient.features.InfoHuds;
import com.blairdash.blueclient.features.Keystrokes;
import com.blairdash.blueclient.features.PvpStats;
import com.blairdash.blueclient.features.ToggleMovement;
import com.blairdash.blueclient.features.PotionHud;
import com.blairdash.blueclient.handler.MenuKeyHandler;
import com.blairdash.blueclient.handler.MotionBlurHandler;
import com.blairdash.blueclient.handler.SelfNametagHandler;
import com.blairdash.blueclient.features.CrosshairOverlay;
import com.blairdash.blueclient.handler.ZoomHandler;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/**
 * Blue Client for 1.8.9, on Forge (11.15.1.2318) + SpongePowered Mixin.
 *
 * 1.8.9 predates Fabric entirely, so this is a from-scratch port rather
 * than a rename: no GuiGraphics/Identifier/split source sets, immediate-
 * mode OpenGL rendering via GuiIngame/EntityRenderer/Tessellator, and
 * Forge's own event bus covers a lot of what needed a Mixin on Fabric
 * (e.g. FOVUpdateEvent replaces the CameraMixin FOV override from the
 * 26.x branches). Every symbol here has been confirmed against the real
 * deobfuscated 1.8.9 jar via javap, the same verification approach used
 * throughout the 26.x ports.
 */
@Mod(modid = BlueClientMod.MODID, name = "Blue Client", version = "1.4.1", clientSideOnly = true)
public class BlueClientMod {
    public static final String MODID = "blueclient";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    private static final String KEY_CATEGORY = "key.categories.blueclient";

    // Opens/closes the mod menu overlay -- Right Shift, matching the 26.x branches.
    public static KeyBinding openMenuKey;
    // Held to zoom, like Lunar/OptiFine's default zoom bind.
    public static KeyBinding zoomKey;
    // Held to look around without turning the player.
    public static KeyBinding freelookKey;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
		try { Class.forName("com.blairdash.blueclient.update.BlueUpdater").getMethod("run").invoke(null); } catch (Throwable ignored) { } // standalone builds only; launcher builds don't ship the updater
        LOGGER.info("Blue Client (1.8.9) pre-init");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        ConfigStorage.load();
        com.blairdash.blueclient.features.CrosshairData.load();
        Display.setTitle("Blue Client 1.8.9 (v1)");
        com.blairdash.blueclient.net.RankService.start();
        com.blairdash.blueclient.LauncherUpdater.start();

        openMenuKey = new KeyBinding("key.blueclient.openmenu", Keyboard.KEY_RSHIFT, KEY_CATEGORY);
        zoomKey = new KeyBinding("key.blueclient.zoom", Keyboard.KEY_C, KEY_CATEGORY);
        freelookKey = new KeyBinding("key.blueclient.freelook", Keyboard.KEY_LMENU, KEY_CATEGORY);
        ClientRegistry.registerKeyBinding(openMenuKey);
        ClientRegistry.registerKeyBinding(freelookKey);
        ClientRegistry.registerKeyBinding(zoomKey);

        MinecraftForge.EVENT_BUS.register(new ZoomHandler());
        MinecraftForge.EVENT_BUS.register(new CrosshairOverlay());
        MinecraftForge.EVENT_BUS.register(new CpsFpsHud());
        MinecraftForge.EVENT_BUS.register(new CoordinatesHud());
        MinecraftForge.EVENT_BUS.register(new PotionHud());
        MinecraftForge.EVENT_BUS.register(new ArmorHud());
        MinecraftForge.EVENT_BUS.register(new Keystrokes());
        MinecraftForge.EVENT_BUS.register(new CheatWatch());
        MinecraftForge.EVENT_BUS.register(new ToggleMovement());
        MinecraftForge.EVENT_BUS.register(new Freelook());
        MinecraftForge.EVENT_BUS.register(new PvpStats());
        MinecraftForge.EVENT_BUS.register(new InfoHuds());
        MinecraftForge.EVENT_BUS.register(new com.blairdash.blueclient.features.ExtraMods());
        MinecraftForge.EVENT_BUS.register(new com.blairdash.blueclient.handler.MenuReplaceHandler());
        MinecraftForge.EVENT_BUS.register(new com.blairdash.blueclient.handler.RankNametags());
        MinecraftForge.EVENT_BUS.register(new MenuKeyHandler());
        MinecraftForge.EVENT_BUS.register(new MotionBlurHandler());
        MinecraftForge.EVENT_BUS.register(new SelfNametagHandler());
        MinecraftForge.EVENT_BUS.register(new com.blairdash.blueclient.handler.RenderStateFixes());
        MinecraftForge.EVENT_BUS.register(new com.blairdash.blueclient.handler.WorldTweakHandler());

        LOGGER.info("Blue Client (1.8.9) init");
    }
}
