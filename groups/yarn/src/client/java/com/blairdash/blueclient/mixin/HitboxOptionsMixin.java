package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.features.HitboxOptions;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Customizes the existing debug hitbox pass, preserving its camera and visibility checks. */
@Mixin(EntityHitboxDebugRenderer.class)
public class HitboxOptionsMixin {
    @Inject(method = "drawHitbox", at = @At("HEAD"), cancellable = true)
    private void blue$hitbox(Entity entity, float tickDelta, boolean serverSide, CallbackInfo ci) {
        if (!ModConfig.hitboxesEnabled) return;
        ci.cancel();
        if (!HitboxOptions.visible(entity)) return;
        Vec3d position = entity.getLerpedPos(tickDelta);
        Box box = entity.getBoundingBox().offset(position.subtract(entity.getEntityPos()));
        int color = HitboxOptions.color(entity);
        float width = HitboxOptions.thickness();
        GizmoDrawing.box(box, DrawStyle.stroked(color, width));
        if (HitboxOptions.eyeLine() && entity instanceof LivingEntity) {
            double eye = box.minY + entity.getStandingEyeHeight();
            GizmoDrawing.box(new Box(box.minX, eye - 0.01, box.minZ, box.maxX, eye + 0.01, box.maxZ),
                    DrawStyle.stroked(0xFFFF0000, width));
        }
        if (HitboxOptions.lookVector()) {
            Vec3d eye = position.add(0, entity.getStandingEyeHeight(), 0);
            GizmoDrawing.arrow(eye, eye.add(entity.getRotationVec(tickDelta).multiply(2)), 0xFF0000FF, width);
        }
    }
}
