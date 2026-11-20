package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Tracks left/right click timestamps over a rolling 1-second window for CPS, plus current FPS. */
public class CpsFpsHud {
    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private static boolean prevAttackDown = false;
    private static boolean prevUseDown = false;

    public static void tick(Minecraft client) {
        if (client.player == null) return;
        long now = System.currentTimeMillis();

        KeyMapping attack = client.options.keyAttack;
        KeyMapping use = client.options.keyUse;

        if (attack.isDown() && !prevAttackDown) leftClicks.addLast(now);
        if (use.isDown() && !prevUseDown) rightClicks.addLast(now);
        prevAttackDown = attack.isDown();
        prevUseDown = use.isDown();

        purgeOld(leftClicks, now);
        purgeOld(rightClicks, now);
    }

    private static void purgeOld(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() > 1000) {
            deque.pollFirst();
        }
    }

    public static int leftCps() { return leftClicks.size(); }
    public static int rightCps() { return rightClicks.size(); }

    public static void render(GuiGraphicsExtractor ctx, Minecraft client) {
        int x = ModConfig.cpsFpsX;
        int y = ModConfig.cpsFpsY;
        int fps = client.getFps();
        int cps = Math.max(leftClicks.size(), rightClicks.size());
        HudStyle.block(ctx, client, java.util.List.of("FPS: " + fps, "CPS: " + cps), null, x, y);
    }
}