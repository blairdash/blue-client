package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lowers the shield in first person (either hand) by shifting the matrix
 * down before the item is rendered and restoring it afterwards, so the
 * other hand and the fire overlay aren't affected.
 */
@Mixin(ItemInHandRenderer.class)
public class LowShieldMixin {

    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void clientmod$lowerShieldStart(AbstractClientPlayer player, float tickProgress, float pitch,
                                            InteractionHand hand, float swingProgress, ItemStack item, float equipProgress,
                                            PoseStack matrices, SubmitNodeCollector orderedRenderCommandQueue,
                                            int light, CallbackInfo ci) {
        if (ModConfig.lowShieldEnabled && item.is(Items.SHIELD)) {
            matrices.pushPose();
            matrices.translate(0.0F, -ModConfig.lowShieldOffset, 0.0F);
        }
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void clientmod$lowerShieldEnd(AbstractClientPlayer player, float tickProgress, float pitch,
                                          InteractionHand hand, float swingProgress, ItemStack item, float equipProgress,
                                          PoseStack matrices, SubmitNodeCollector orderedRenderCommandQueue,
                                          int light, CallbackInfo ci) {
        if (ModConfig.lowShieldEnabled && item.is(Items.SHIELD)) {
            matrices.popPose();
        }
    }
}