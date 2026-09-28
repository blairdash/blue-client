package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lowers the shield in first person (either hand) by shifting the matrix
 * down before the item is rendered and restoring it afterwards, so the
 * other hand and the fire overlay aren't affected.
 */
@Mixin(HeldItemRenderer.class)
public class LowShieldMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void clientmod$lowerShieldStart(AbstractClientPlayerEntity player, float tickProgress, float pitch,
                                            Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                            MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue,
                                            int light, CallbackInfo ci) {
        if (ModConfig.lowShieldEnabled && item.isOf(Items.SHIELD)) {
            matrices.push();
            matrices.translate(0.0F, -ModConfig.lowShieldOffset, 0.0F);
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void clientmod$lowerShieldEnd(AbstractClientPlayerEntity player, float tickProgress, float pitch,
                                          Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                          MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue,
                                          int light, CallbackInfo ci) {
        if (ModConfig.lowShieldEnabled && item.isOf(Items.SHIELD)) {
            matrices.pop();
        }
    }
}