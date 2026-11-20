package com.blairdash.blueclient.features;

import com.blairdash.blueclient.BlueClientMod;
import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Hold the freelook key to swing the camera around the player without turning
 * them. No mixins: each frame the mouse movement is read from the game's mouse
 * helper into our own camera angles, the player's real rotation is put back
 * after the frame's mouse handling, and the freelook angles are swapped in for
 * exactly the span of EntityRenderer.orientCamera (between Forge's FOVModifier
 * event, which fires just before it, and its CameraSetup event).
 */
public class Freelook {
    public static boolean active = false;
    public static float yaw, pitch;

    private static int previousView = 0;

    // The player's real rotation at the start of the current frame.
    private static float sYaw, sPrevYaw, sPitch, sPrevPitch;
    private static boolean haveStart = false;
    private static boolean deltaApplied = false;
    private static boolean swapped = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        boolean want = ModConfig.freelookEnabled && mc.thePlayer != null && mc.currentScreen == null
                && BlueClientMod.freelookKey != null && BlueClientMod.freelookKey.isKeyDown();
        if (want && !active) {
            active = true;
            yaw = mc.thePlayer.rotationYaw;
            pitch = mc.thePlayer.rotationPitch;
            previousView = mc.gameSettings.thirdPersonView;
            mc.gameSettings.thirdPersonView = 1; // behind the player
        } else if (!want && active) {
            active = false;
            mc.gameSettings.thirdPersonView = previousView;
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.thePlayer;
        if (event.phase == TickEvent.Phase.START) {
            deltaApplied = false;
            swapped = false;
            haveStart = false;
            if (active && p != null) {
                sYaw = p.rotationYaw;
                sPrevYaw = p.prevRotationYaw;
                sPitch = p.rotationPitch;
                sPrevPitch = p.prevRotationPitch;
                haveStart = true;
            }
        } else if (haveStart && p != null) {
            // Undo this frame's mouse turn (and any leftover swap): the player must not turn.
            restore(p);
            swapped = false;
            haveStart = false;
        }
    }

    @SubscribeEvent
    public void onFov(EntityViewRenderEvent.FOVModifier event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.thePlayer;
        if (!active || !haveStart || p == null || event.entity != p) return;

        if (!deltaApplied) {
            deltaApplied = true;
            if (mc.inGameHasFocus && mc.currentScreen == null) {
                // Same maths as EntityRenderer.updateCameraAndRender / Entity.setAngles.
                float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
                float f1 = f * f * f * 8.0F;
                float dx = mc.mouseHelper.deltaX * f1;
                float dy = mc.mouseHelper.deltaY * f1 * (mc.gameSettings.invertMouse ? -1 : 1);
                yaw += dx * 0.15f;
                pitch = Math.max(-90f, Math.min(90f, pitch - dy * 0.15f));
            }
        }

        // Swap the freelook angles in; CameraSetup (inside orientCamera) swaps them back out.
        p.rotationYaw = p.prevRotationYaw = yaw;
        p.rotationPitch = p.prevRotationPitch = pitch;
        swapped = true;
    }

    @SubscribeEvent
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (swapped && haveStart && p != null) {
            restore(p);
            swapped = false;
        }
    }

    private static void restore(EntityPlayerSP p) {
        p.rotationYaw = sYaw;
        p.prevRotationYaw = sPrevYaw;
        p.rotationPitch = sPitch;
        p.prevRotationPitch = sPrevPitch;
    }
}
