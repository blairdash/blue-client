package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class ItemCustomizerMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void blue$start(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress,
                            ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider queue, int light, CallbackInfo ci) {
        BlueMod m = VisualMods.active("item_customizer");
        if (m == null || item.isEmpty()) return;
        matrices.push();
        float side = hand == Hand.MAIN_HAND ? 1f : -1f;
        matrices.translate((float) m.setting("x").n * side, (float) m.setting("y").n, (float) m.setting("z").n);
        float s = (float) m.setting("scale").n;
        matrices.scale(s, s, s);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"), require = 0)
    private void blue$end(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress,
                          ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider queue, int light, CallbackInfo ci) {
        if (VisualMods.active("item_customizer") != null && !item.isEmpty()) matrices.pop();
    }
}
