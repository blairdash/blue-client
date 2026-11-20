package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.gui.BlueMenuConfig;
import com.blairdash.blueclient.gui.BlueTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the chosen menu theme behind every out-of-world screen (Singleplayer, Multiplayer, Options...). */
@Mixin(Screen.class)
public class ScreenThemeBackgroundMixin {
    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "extractBackground", at = @At("RETURN"))
    private void clientmod$theme(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (!((Object) this instanceof BlueTitleScreen)) BlueTitleScreen.watermark(context, width, height);
        int theme = BlueMenuConfig.blueMenu() ? BlueMenuConfig.theme() : 0;
        if (Minecraft.getInstance().level != null || theme == 0) return;
        BlueTitleScreen.paintTheme(context, theme, 0, 0, width, height);
    }
}
