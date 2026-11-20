package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.net.RankService;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class SelfNametagIconMixin {

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void blueclient$prependRankStar(Entity entity, CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(com.blairdash.blueclient.gui.NametagIcon.decorate(entity, cir.getReturnValue()));
    }
}
