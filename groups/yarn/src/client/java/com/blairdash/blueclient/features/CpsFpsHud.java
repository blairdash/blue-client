package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayDeque;
import java.util.Deque;

/** Tracks left/right click timestamps over a rolling 1-second window for CPS, plus current FPS. */
public class CpsFpsHud {
    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private static boolean prevAttackDown = false;
    private static boolean prevUseDown = false;

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;
        long now = System.currentTimeMillis();

        KeyBinding attack = client.options.attackKey;
        KeyBinding use = client.options.useKey;

        if (attack.isPressed() && !prevAttackDown) leftClicks.addLast(now);
        if (use.isPressed() && !prevUseDown) rightClicks.addLast(now);
        prevAttackDown = attack.isPressed();
        prevUseDown = use.isPressed();

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

    public static void render(DrawContext ctx, MinecraftClient client) {
        int x = ModConfig.cpsFpsX;
        int y = ModConfig.cpsFpsY;
        int fps = client.getCurrentFps();
        int cps = Math.max(leftClicks.size(), rightClicks.size());
        HudStyle.block(ctx, client, java.util.List.of("FPS: " + fps, "CPS: " + cps), null, x, y);
    }
}