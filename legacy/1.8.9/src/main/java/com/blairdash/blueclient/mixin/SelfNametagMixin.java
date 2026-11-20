package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the local player's own name tag render in third person by way of
 * vanilla's own, already-correct rendering path, instead of reimplementing
 * it. Confirmed via javap: RendererLivingEntity#canRenderName(EntityLivingBase)
 * always returns false when the entity being checked is
 * RenderManager#livingPlayer (i.e. yourself) -- the only thing standing
 * between "renders normally" and "never renders" -- and confirmed separately
 * that in third person (thirdPersonView != 0) the local player already goes
 * through the exact same Render/RendererLivingEntity#doRender pipeline as
 * every other entity (RenderGlobal only excludes the view entity from that
 * pipeline in first person). So unlocking this one guard is enough to get a
 * fully correct, camera-tracking third-person name tag for free, with none
 * of the custom billboard math SelfNametagHandler needs for first person
 * (where the local player is skipped by that pipeline entirely).
 */
@Mixin(RendererLivingEntity.class)
public abstract class SelfNametagMixin {

    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("RETURN"), cancellable = true)
    private void blueclient$allowSelfNametag(EntityLivingBase entity, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.selfNametagEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (entity != mc.thePlayer || mc.gameSettings.thirdPersonView == 0) {
            return;
        }
        // Recomputing unconditionally (rather than short-circuiting when the
        // original result was already true) sidesteps needing
        // CallbackInfoReturnable's primitive unboxing helper, whose exact
        // name varies across Mixin versions -- and is harmless here since
        // the recomputed value is what vanilla would already return for any
        // other entity in this same spot.
        boolean canRender = Minecraft.isGuiEnabled()
                && !entity.isInvisibleToPlayer(mc.thePlayer)
                && entity.riddenByEntity == null;
        cir.setReturnValue(canRender);
    }
}
