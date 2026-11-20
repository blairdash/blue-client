package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.features.HitboxOptions;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the Hitbox options module (per-category visibility and colors, eye line, look vector). */
@Mixin(EntityRenderDispatcher.class)
public class HitboxOptionsMixin {
    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true, require = 0)
    private static void blue$hitbox(MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta, CallbackInfo ci) {
        if (!ModConfig.hitboxesEnabled) return;
        ci.cancel();
        if (!HitboxOptions.visible(entity)) return;
        Box box = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());
        int color = HitboxOptions.color(entity);
        WorldRenderer.drawBox(matrices, vertices, box, ((color >> 16) & 255) / 255f,
                ((color >> 8) & 255) / 255f, (color & 255) / 255f, ((color >>> 24) & 255) / 255f);
        if (HitboxOptions.eyeLine() && entity instanceof LivingEntity) {
            double eye = entity.getStandingEyeHeight();
            WorldRenderer.drawBox(matrices, vertices, new Box(box.minX, eye - 0.01, box.minZ, box.maxX, eye + 0.01, box.maxZ), 1, 0, 0, 1);
        }
        if (HitboxOptions.lookVector()) {
            Vec3d view = entity.getRotationVec(tickDelta);
            MatrixStack.Entry entry = matrices.peek();
            Matrix4f pos = entry.getPositionMatrix();
            float ey = entity.getStandingEyeHeight();
            vertices.vertex(pos, 0, ey, 0).color(0, 0, 255, 255).normal(entry, (float) view.x, (float) view.y, (float) view.z).next();
            vertices.vertex(pos, (float) (view.x * 2), ey + (float) (view.y * 2), (float) (view.z * 2)).color(0, 0, 255, 255).normal(entry, (float) view.x, (float) view.y, (float) view.z).next();
        }
    }
}