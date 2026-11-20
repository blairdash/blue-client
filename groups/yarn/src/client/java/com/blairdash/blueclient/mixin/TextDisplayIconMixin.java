package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.gui.NametagIcon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rank icon on floating Text Display tags (e.g. TAB) that ride a Blue Client player. */
@Mixin(DisplayEntity.TextDisplayEntity.class)
public abstract class TextDisplayIconMixin {
    @Inject(method = "getText", at = @At("RETURN"), cancellable = true)
    private void blueclient$rankIcon(CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(NametagIcon.decorate((Entity) (Object) this, cir.getReturnValue()));
    }
}