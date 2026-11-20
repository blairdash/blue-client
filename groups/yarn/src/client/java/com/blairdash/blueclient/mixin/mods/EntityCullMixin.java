package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FPS Boost: stops drawing mobs, items, frames and stands that are far away. */
@Mixin(EntityRenderer.class)
public class EntityCullMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void blue$cull(Entity e, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        BlueMod m = VisualMods.active("fps_boost");
        if (m == null || !m.setting("cull").b) return;
        double limit;
        if (e instanceof ItemEntity || e instanceof AbstractDecorationEntity || e instanceof ArmorStandEntity || e instanceof ExperienceOrbEntity) limit = m.setting("cull_items").n;
        else if (e instanceof MobEntity && !(e instanceof EnderDragonEntity) && !(e instanceof WitherEntity)) limit = m.setting("cull_mobs").n;
        else return;
        double dx = e.getX() - camX, dy = e.getY() - camY, dz = e.getZ() - camZ;
        if (dx * dx + dy * dy + dz * dz > limit * limit) { cir.setReturnValue(false); return; }
        if (m.setting("occlusion").b && !e.isGlowing()
                && com.blairdash.blueclient.mods.OcclusionCull.hidden(e, camX, camY, camZ, m.setting("occ_min").n)) cir.setReturnValue(false);
    }
}
