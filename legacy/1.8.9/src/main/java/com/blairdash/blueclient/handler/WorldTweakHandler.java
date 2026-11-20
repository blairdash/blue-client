package com.blairdash.blueclient.handler;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Fullbright, No Hurt Cam, Time Changer, Clear Weather and Chat Timestamps.
 * Everything is client-side only; the server is never told.
 */
public class WorldTweakHandler {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private float savedGamma = 1.0f;
    private boolean gammaOverridden;
    private int savedHurtTime = -1;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();

        // Fullbright: push gamma way up, and put the player's own value back when turned off.
        if (ModConfig.fullbrightEnabled) {
            if (!gammaOverridden) { savedGamma = mc.gameSettings.gammaSetting; gammaOverridden = true; }
            mc.gameSettings.gammaSetting = 1000.0f;
        } else if (gammaOverridden) {
            mc.gameSettings.gammaSetting = savedGamma;
            gammaOverridden = false;
        }

        World w = mc.theWorld;
        if (w == null) return;
        applyWorld(w);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.phase == TickEvent.Phase.START) {
            if (mc.theWorld != null) applyWorld(mc.theWorld);
            // No Hurt Cam: the camera shake is driven by hurtTime, so hide it for the frame.
            EntityPlayer p = mc.thePlayer;
            if (ModConfig.noHurtCamEnabled && p != null) {
                savedHurtTime = p.hurtTime;
                p.hurtTime = 0;
            }
        } else if (savedHurtTime >= 0) {
            if (mc.thePlayer != null) mc.thePlayer.hurtTime = savedHurtTime;
            savedHurtTime = -1;
        }
    }

    private void applyWorld(World w) {
        if (ModConfig.timeChangerEnabled) {
            w.setWorldTime((long) ModConfig.timeOfDayValue);
        }
        if (ModConfig.clearWeatherEnabled) {
            w.prevRainingStrength = 0.0f;
            w.rainingStrength = 0.0f;
            w.prevThunderingStrength = 0.0f;
            w.thunderingStrength = 0.0f;
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (!ModConfig.chatTimestampsEnabled || e.type == 2) return;
        // Colour codes inside the text, not a ChatStyle: 1.8.9's chat line splitter concatenates each component's code
        // with no reset after it, so a styled grey stamp leaks its grey into the whole message. The explicit reset
        // (section-r) ends it. The stamp and the message are siblings under an empty root so neither inherits the other.
        IChatComponent stamp = new ChatComponentText("\u00a77[" + LocalTime.now().format(CLOCK) + "] \u00a7r");
        IChatComponent root = new ChatComponentText("");
        root.appendSibling(stamp);
        root.appendSibling(e.message);
        e.message = root;
    }
}