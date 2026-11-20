package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.gui.BlueMenuConfig;
import com.blairdash.blueclient.gui.BlueTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the chosen menu theme behind every out-of-world screen (Singleplayer, Multiplayer, Options...). */
@Mixin(Screen.class)
public class ScreenThemeBackgroundMixin {
    @Inject(method = "renderBackground", at = @At("RETURN"))
    private void clientmod$theme(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this instanceof BlueTitleScreen)) BlueTitleScreen.watermark(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        MinecraftClient mc = MinecraftClient.getInstance();
        int theme = BlueMenuConfig.blueMenu() ? BlueMenuConfig.theme() : 0;
        if (mc.world != null || theme == 0) return;
        BlueTitleScreen.paintTheme(context, theme, 0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight());
    }
}
