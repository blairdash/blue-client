package com.blairdash.blueclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Base for Blue's own menu screens: hides the per-version render signature behind {@link #draw}. */
public abstract class BlueScreen extends Screen {
    protected BlueScreen(Component title) { super(title); }

    protected abstract void draw(GuiGraphicsExtractor g, int mx, int my, float delta);

    @Override
    public void extractRenderState(GuiGraphicsExtractor g) {
        Ui.res = 1;
        draw(g, g.mouseX(), g.mouseY(), g.partialTicks());
    }

    /** Vanilla background/widgets of this screen. */
    protected void drawVanilla(GuiGraphicsExtractor g, int mx, int my, float delta) { super.extractRenderState(g); }

    /** Draws another screen (used for the popup's backdrop). */
    static void drawChild(Screen s, GuiGraphicsExtractor g, int mx, int my, float delta) { s.extractRenderState(g); }
}
