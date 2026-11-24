package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.gui.RankIcon;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.Style;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Draws the Blue Client badge as a textured quad (no text shadow) wherever the text drawer reaches a spacer glyph: nametags, Text Displays, GUI text. */
@Mixin(targets = "net.minecraft.client.font.TextRenderer$Drawer")
public abstract class TextDrawerBadgeMixin {
    @Shadow @Final private VertexConsumerProvider vertexConsumers;
    @Shadow @Final private boolean shadow;
    @Shadow @Final private int color;
    @Shadow @Final private Matrix4f matrix;
    @Shadow @Final private TextRenderer.TextLayerType layerType;
    @Shadow @Final private int light;
    @Shadow float x;
    @Shadow float y;

    @Inject(method = "accept(ILnet/minecraft/text/Style;I)Z", at = @At("HEAD"))
    private void blueclient$badge(int index, Style style, int cp, CallbackInfoReturnable<Boolean> cir) {
        if (shadow || cp < 0xE010 || cp > 0xE014) return;
        float alpha = ((color >>> 24) & 0xFF) / 255f;
        RankIcon.worldBadge(vertexConsumers, layerType, matrix, x, y, alpha <= 0f ? 1f : alpha, light, style, cp);
    }
}