package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FPS Boost: stops drawing mobs, items, frames and stands that are far away. */
@Mixin(EntityRenderer.class)
public class EntityCullMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void blue$cull(Entity e, Frustum frustum, double camX, double camY, double camZ, float partialTick, CallbackInfoReturnable<Boolean> cir) {
        BlueMod m = VisualMods.active("fps_boost");
        if (m == null || !m.setting("cull").b) return;
        double limit;
        if (e instanceof ItemEntity || e instanceof HangingEntity || e instanceof ArmorStand || e instanceof ExperienceOrb) limit = m.setting("cull_items").n;
        else if (e instanceof Mob && !(e instanceof EnderDragon) && !(e instanceof WitherBoss)) limit = m.setting("cull_mobs").n;
        else return;
        double dx = e.getX() - camX, dy = e.getY() - camY, dz = e.getZ() - camZ;
        if (dx * dx + dy * dy + dz * dz > limit * limit) { cir.setReturnValue(false); return; }
        if (m.setting("occlusion").b && !e.isCurrentlyGlowing()
                && com.blairdash.blueclient.mods.OcclusionCull.hidden(e, camX, camY, camZ, m.setting("occ_min").n)) cir.setReturnValue(false);
    }
}
