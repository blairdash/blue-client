package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Shows W / A / S / D and the left / right mouse buttons. A key lights up
 * while it's held. Uses the game's own key bindings, so remapped keys work.
 *
 *        [ W ]
 *   [ A ] [ S ] [ D ]
 *   [ LMB ] [ RMB ]
 */
public class Keystrokes {
    private static final int KEY = 22;    // key size in scaled GUI pixels
    private static final int GAP = 2;
    private static final int DEFAULT_X = 6;

    private static final int IDLE_BG = 0x80000000;
    private static final int PRESSED_BG = 0xCCFFFFFF;
    private static final int IDLE_TEXT = 0xFFFFFFFF;
    private static final int PRESSED_TEXT = 0xFF000000;

    public static int width() {
        return 3 * KEY + 2 * GAP;
    }

    public static int height() {
        return 3 * KEY + 2 * GAP;
    }

    public static int effectiveX() {
        return ModConfig.keystrokesX >= 0 ? ModConfig.keystrokesX : DEFAULT_X;
    }

    public static int effectiveY(int scaledHeight) {
        return ModConfig.keystrokesY >= 0 ? ModConfig.keystrokesY : (scaledHeight - height()) / 2;
    }

    @SubscribeEvent
    public void onRenderPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!ModConfig.keystrokesEnabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.hideGUI) return;

        int x = effectiveX();
        int y = effectiveY(event.resolution.getScaledHeight());
        int step = KEY + GAP;

        // Row 1: W
        drawKey(mc, x + step, y, KEY, KEY, "W", mc.gameSettings.keyBindForward.isKeyDown());

        // Row 2: A S D
        int y2 = y + step;
        drawKey(mc, x, y2, KEY, KEY, "A", mc.gameSettings.keyBindLeft.isKeyDown());
        drawKey(mc, x + step, y2, KEY, KEY, "S", mc.gameSettings.keyBindBack.isKeyDown());
        drawKey(mc, x + 2 * step, y2, KEY, KEY, "D", mc.gameSettings.keyBindRight.isKeyDown());

        // Row 3: LMB RMB (together exactly as wide as the rows above)
        int y3 = y2 + step;
        int mouseWidth = (width() - GAP) / 2;
        drawKey(mc, x, y3, mouseWidth, KEY, "LMB", mc.gameSettings.keyBindAttack.isKeyDown());
        drawKey(mc, x + mouseWidth + GAP, y3, mouseWidth, KEY, "RMB", mc.gameSettings.keyBindUseItem.isKeyDown());
    }

    private static void drawKey(Minecraft mc, int x, int y, int w, int h, String label, boolean pressed) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawRect(x, y, x + w, y + h, pressed ? PRESSED_BG : IDLE_BG);
        GlStateManager.disableBlend();

        int textWidth = mc.fontRendererObj.getStringWidth(label);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (h - 8) / 2;
        // Dark text on a white key looks smudged with a shadow, so only shadow idle keys.
        if (pressed) {
            mc.fontRendererObj.drawString(label, textX, textY, PRESSED_TEXT);
        } else {
            mc.fontRendererObj.drawStringWithShadow(label, textX, textY, IDLE_TEXT);
        }
    }
}
