package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.gui.NametagIcon;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rank icon on floating Text Display tags (e.g. TAB) that ride a Blue Client player. */
@Mixin(Display.TextDisplay.class)
public abstract class TextDisplayIconMixin {
    @Inject(method = "getText", at = @At("RETURN"), cancellable = true)
    private void blueclient$rankIcon(CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(NametagIcon.decorate((Entity) (Object) this, cir.getReturnValue()));
    }
}