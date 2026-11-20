package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.features.HitboxOptions;
import com.blairdash.blueclient.features.HitboxLines;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class HitboxOptionsMixin {
    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/RenderLayer;getLines()Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer blue$lineWidth(RenderLayer original) {
        return ModConfig.hitboxesEnabled ? HitboxLines.layer(HitboxOptions.thickness()) : original;
    }

    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void blue$hitbox(MatrixStack matrices, VertexConsumer vertices, Entity entity,
                                    float tickDelta, float red, float green, float blue, CallbackInfo ci) {
        if (!ModConfig.hitboxesEnabled) return;
        ci.cancel();
        if (!HitboxOptions.visible(entity)) return;
        Box box = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());
        int color = HitboxOptions.color(entity);
        VertexRendering.drawBox(matrices, vertices, box, ((color >> 16) & 255) / 255f,
                ((color >> 8) & 255) / 255f, (color & 255) / 255f, ((color >>> 24) & 255) / 255f);
        if (HitboxOptions.eyeLine() && entity instanceof LivingEntity) {
            double eye = entity.getStandingEyeHeight();
            VertexRendering.drawBox(matrices, vertices, new Box(box.minX, eye - 0.01, box.minZ,
                    box.maxX, eye + 0.01, box.maxZ), 1, 0, 0, 1);
        }
        if (HitboxOptions.lookVector()) {
            VertexRendering.drawVector(matrices, vertices, new Vector3f(0, entity.getStandingEyeHeight(), 0),
                    entity.getRotationVec(tickDelta).multiply(2), 0xFF0000FF);
        }
    }
}
