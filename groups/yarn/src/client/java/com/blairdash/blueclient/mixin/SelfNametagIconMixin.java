package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.net.RankService;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class SelfNametagIconMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void blueclient$prependRankStar(Entity entity, CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(com.blairdash.blueclient.gui.NametagIcon.decorate(entity, cir.getReturnValue()));
    }
}