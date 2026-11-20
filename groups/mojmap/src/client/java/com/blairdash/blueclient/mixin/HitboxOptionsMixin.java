package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import com.blairdash.blueclient.features.HitboxOptions;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gizmos.Gizmos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Customizes the existing debug hitbox pass, preserving its camera and visibility checks. */
@Mixin(EntityHitboxDebugRenderer.class)
public class HitboxOptionsMixin {
    @Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true)
    private void blue$hitbox(Entity entity, float tickDelta, boolean serverSide, CallbackInfo ci) {
        if (!ModConfig.hitboxesEnabled) return;
        ci.cancel();
        if (!HitboxOptions.visible(entity)) return;
        Vec3 position = entity.getPosition(tickDelta);
        AABB box = entity.getBoundingBox().move(position.subtract(entity.position()));
        int color = HitboxOptions.color(entity);
        float width = HitboxOptions.thickness();
        Gizmos.cuboid(box, GizmoStyle.stroke(color, width));
        if (HitboxOptions.eyeLine() && entity instanceof LivingEntity) {
            double eye = box.minY + entity.getEyeHeight();
            Gizmos.cuboid(new AABB(box.minX, eye - 0.01, box.minZ, box.maxX, eye + 0.01, box.maxZ),
                    GizmoStyle.stroke(0xFFFF0000, width));
        }
        if (HitboxOptions.lookVector()) {
            Vec3 eye = position.add(0, entity.getEyeHeight(), 0);
            Gizmos.arrow(eye, eye.add(entity.getViewVector(tickDelta).scale(2)), 0xFF0000FF, width);
        }
    }
}
