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
    private Screen clientmod$blueTitle(Screen screen) {
        return screen != null && BlueMenuConfig.blueMenu() && screen.getClass() == TitleScreen.class ? new BlueTitleScreen() : screen;
    }
}
