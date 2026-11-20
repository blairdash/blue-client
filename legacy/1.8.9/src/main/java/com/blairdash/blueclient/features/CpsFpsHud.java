package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Tracks left/right click timestamps over a rolling 1-second window for CPS, plus current FPS. */
public class CpsFpsHud {
    private static final Deque<Long> leftClicks = new ArrayDeque<Long>();
    private static final Deque<Long> rightClicks = new ArrayDeque<Long>();
    private static boolean prevAttackDown = false;
    private static boolean prevUseDown = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        long now = System.currentTimeMillis();

        KeyBinding attack = mc.gameSettings.keyBindAttack;
        KeyBinding use = mc.gameSettings.keyBindUseItem;

        if (attack.isKeyDown() && !prevAttackDown) leftClicks.addLast(now);
        if (use.isKeyDown() && !prevUseDown) rightClicks.addLast(now);
        prevAttackDown = attack.isKeyDown();
        prevUseDown = use.isKeyDown();

        purgeOld(leftClicks, now);
        purgeOld(rightClicks, now);
    }

    private static void purgeOld(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() > 1000) {
            deque.pollFirst();
        }
    }

    @SubscribeEvent
    public void onRenderPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!ModConfig.cpsFpsHudEnabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.hideGUI || mc.thePlayer == null) return;

        int x = ModConfig.cpsFpsX;
        int y = ModConfig.cpsFpsY;
        int fps = Minecraft.getDebugFPS();
        int cps = Math.max(leftClicks.size(), rightClicks.size());

        mc.fontRendererObj.drawStringWithShadow("FPS: " + fps, x, y, 0xFFFFFFFF);
        mc.fontRendererObj.drawStringWithShadow("CPS: " + cps, x, y + 10, 0xFFFFFFFF);
    }
}
