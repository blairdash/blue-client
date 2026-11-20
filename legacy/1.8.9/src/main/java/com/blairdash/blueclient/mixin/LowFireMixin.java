package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lowers the on-fire flame overlay rendered around a burning entity (self in
 * third person, or any other burning player/mob) so it covers less of the
 * view during a fight. Confirmed via javap that Render#renderEntityOnFire
 * builds its quad stack entirely from a local origin set by one
 * GlStateManager.scale(F,F,F) call near the top of the method (scale factor
 * = entity.width * 1.4); everything the loop draws afterward is relative to
 * that scaled space, so one extra downward translate right after that scale
 * call shifts the whole stack down without touching the per-vertex math.
 */
@Mixin(Render.class)
public abstract class LowFireMixin {

    @Inject(
        method = "renderEntityOnFire",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V",
            shift = At.Shift.AFTER
        )
    )
    private void blueclient$lowerFire(Entity entityIn, double x, double y, double z, float partialTicks, CallbackInfo ci) {
        if (ModConfig.lowFireEnabled) {
            GlStateManager.translate(0.0F, -ModConfig.lowFireOffset, 0.0F);
        }
    }
}
