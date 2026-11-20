package com.blairdash.blueclient.handler;

import com.blairdash.blueclient.BlueClientMod;
import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * IMPORTANT: 1.8.9 Forge does NOT actually fire net.minecraftforge.client.
 * event.FOVUpdateEvent from its real FOV pipeline -- that class is dead
 * code in this Forge build. Confirmed by disassembling EntityRenderer:
 * it computes the FOV in degrees and hands it to
 * ForgeHooksClient.getFOVModifier(...), which fires
 * EntityViewRenderEvent.FOVModifier instead (getFOV()/setFOV(float), same
 * degrees semantics: default 70, lower = zoomed in). That's the real hook.
 *
 * Also restores the scroll-wheel zoom-level adjustment from the 26.x
 * branches (there implemented as a MouseMixin on MouseHandler.onScroll;
 * here it's Forge's own cancelable MouseEvent, since 1.8.9 has no
 * equivalent mixin target for this).
 */
public class ZoomHandler {
    private static final double MIN_ZOOM_FOV = 4.0;   // most zoomed in
    private static final double MAX_ZOOM_FOV = 60.0;  // least zoomed in
    private static final double STEP = 0.9;           // 10% per notch

    private double current = -1; // -1 = not zoomed right now

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.zoomEnabled || mc.thePlayer == null || BlueClientMod.zoomKey == null
                || !BlueClientMod.zoomKey.isKeyDown()) {
            current = -1;
            return;
        }
        double base = current < 0 ? mc.gameSettings.fovSetting : current;
        current = lerp(base, ModConfig.zoomFov, 0.5);
    }

    @SubscribeEvent
    public void onFovModifier(EntityViewRenderEvent.FOVModifier event) {
        if (current >= 0) {
            event.setFOV((float) current);
        }
    }

    /** While the zoom key is held, the scroll wheel adjusts the zoom level
     *  instead of switching hotbar slots. Scroll up = zoom in, scroll down =
     *  zoom out. The level is remembered until you quit the game. */
    @SubscribeEvent
    public void onMouseScroll(MouseEvent event) {
        if (event.dwheel == 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.zoomEnabled || BlueClientMod.zoomKey == null || !BlueClientMod.zoomKey.isKeyDown()) {
            return;
        }
        if (mc.currentScreen != null) return;

        // dwheel > 0 (scroll up) shrinks the FOV = zoom in. Matches vanilla's
        // own hotbar-scroll normalization (Minecraft.class runTick): sign
        // only, not the raw platform-dependent wheel-delta magnitude.
        int steps = event.dwheel > 0 ? 1 : -1;
        double fov = ModConfig.zoomFov * Math.pow(STEP, steps);
        ModConfig.zoomFov = Math.max(MIN_ZOOM_FOV, Math.min(MAX_ZOOM_FOV, fov));

        event.setCanceled(true); // don't also scroll the hotbar
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
}
