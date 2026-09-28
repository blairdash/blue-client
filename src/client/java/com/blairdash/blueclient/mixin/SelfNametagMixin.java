package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PlayerEntityRenderer overrides EntityRenderer's hasLabel with its own
 * version (parameter type PlayerLikeEntity, not Entity) -- that's the one
 * that actually runs for players, so this has to target this class
 * specifically rather than the base EntityRenderer.
 */
@Mixin(PlayerEntityRenderer.class)
public class SelfNametagMixin {

    @Inject(method = "hasLabel", at = @At("RETURN"), cancellable = true)
    private void clientmod$forceOwnLabel(PlayerLikeEntity entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.selfNametagEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity == client.player && !client.options.getPerspective().isFirstPerson()) {
            cir.setReturnValue(true);
        }
    }
}