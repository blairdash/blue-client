package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
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
 *
 * In 26.3, Mojang renamed ItemInHandRenderer to
 * FirstPersonHandsAndItemsRenderer, and submitArmWithItem gained a leading
 * PlayerRenderState/FirstPersonHandsAndItemsRenderState pair in place of
 * the old AbstractClientPlayer parameter (confirmed via javap against the
 * real 26.3 client jar).
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class LowShieldMixin {

    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void clientmod$lowerShieldStart(PlayerRenderState playerRenderState, FirstPersonHandsAndItemsRenderState renderState,
                                            float tickProgress, float pitch, InteractionHand hand, float swingProgress,
                                            ItemStack item, float equipProgress, PoseStack matrices,
                                            SubmitNodeCollector orderedRenderCommandQueue, int light, CallbackInfo ci) {
        if (ModConfig.lowShieldEnabled && item.is(Items.SHIELD)) {
            matrices.pushPose();
            matrices.translate(0.0F, -ModConfig.lowShieldOffset, 0.0F);
        }
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void clientmod$lowerShieldEnd(PlayerRenderState playerRenderState, FirstPersonHandsAndItemsRenderState renderState,
                                          float tickProgress, float pitch, InteractionHand hand, float swingProgress,
                                          ItemStack item, float equipProgress, PoseStack matrices,
                                          SubmitNodeCollector orderedRenderCommandQueue, int light, CallbackInfo ci) {
        if (ModConfig.lowShieldEnabled && item.is(Items.SHIELD)) {
            matrices.popPose();
        }
    }
}
