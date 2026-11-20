package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class ItemCustomizerMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), require = 0)
    private void blue$start(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState renderState, float partialTick, float pitch, InteractionHand hand, float swingProgress,
                            ItemStack item, float equipProgress, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        BlueMod m = VisualMods.active("item_customizer");
        if (m == null || item.isEmpty()) return;
        poseStack.pushPose();
        float side = hand == InteractionHand.MAIN_HAND ? 1f : -1f;
        poseStack.translate((float) m.setting("x").n * side, (float) m.setting("y").n, (float) m.setting("z").n);
        float s = (float) m.setting("scale").n;
        poseStack.scale(s, s, s);
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"), require = 0)
    private void blue$end(PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState renderState, float partialTick, float pitch, InteractionHand hand, float swingProgress,
                          ItemStack item, float equipProgress, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (VisualMods.active("item_customizer") != null && !item.isEmpty()) poseStack.popPose();
    }
}
