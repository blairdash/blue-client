package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * hasLabel is declared on EntityRenderer<T extends Entity, ...>, but in
 * 1.21.4 PlayerEntityRenderer doesn't override it -- LivingEntityRenderer
 * does (narrowing T to LivingEntity), and PlayerEntityRenderer just
 * inherits that. So the version that actually runs for players lives on
 * LivingEntityRenderer, not PlayerEntityRenderer or EntityRenderer, and
 * that's what this has to target.
 *
 * LivingEntityRenderer generates both the real method (erasure
 * hasLabel(LivingEntity, double)) and a synthetic bridge
 * (hasLabel(Entity, double), to satisfy EntityRenderer's own erasure) that
 * just casts and calls the real one -- the explicit descriptor below picks
 * the real one, since a bare method name would be ambiguous between them.
 */
@Mixin(LivingEntityRenderer.class)
public class SelfNametagMixin {

    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("RETURN"), cancellable = true)
    private void clientmod$forceOwnLabel(LivingEntity entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.selfNametagEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity == client.player && !client.options.getPerspective().isFirstPerson()) {
            cir.setReturnValue(true);
        }
    }
}
