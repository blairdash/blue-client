package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.gui.BlueMenuConfig;
import com.blairdash.blueclient.gui.BlueTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Swaps vanilla's TitleScreen for the Blue main menu. */
@Mixin(MinecraftClient.class)
public class TitleScreenReplaceMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen clientmodblueTitle(Screen screen) {
        if (!BlueMenuConfig.blueMenu()) return screen;
        MinecraftClient mc = MinecraftClient.getInstance();
        // closing a menu with no world loaded makes vanilla open its own TitleScreen after this hook, so catch null too
        if (screen == null && mc.world == null) return new BlueTitleScreen();
        return screen != null && screen.getClass() == TitleScreen.class ? new BlueTitleScreen() : screen;
    }
}