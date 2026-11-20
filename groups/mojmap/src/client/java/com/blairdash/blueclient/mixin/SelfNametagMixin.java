package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AvatarRenderer overrides EntityRenderer's shouldShowName with its own
 * version (parameter type Avatar, not Entity) -- that's the one
 * that actually runs for players, so this has to target this class
 * specifically rather than the base EntityRenderer.
 */
@Mixin(AvatarRenderer.class)
public class SelfNametagMixin {

    @Inject(method = "shouldShowName", at = @At("RETURN"), cancellable = true)
    private void clientmod$forceOwnLabel(Avatar entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.selfNametagEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (entity == client.player && !client.options.getCameraType().isFirstPerson()) {
            cir.setReturnValue(true);
        }
    }
}