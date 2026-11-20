package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.features.Freelook;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Points the camera at the freelook angles instead of the player's own.
 * Hooks setRotation itself (rather than one call site in update), so it
 * works no matter which method the game uses to place the camera.
 */
@Mixin(Camera.class)
public class FreelookCameraMixin {
    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, index = 1)
    private float clientmod$freelookYaw(float yaw) {
        return Freelook.active ? Freelook.yaw : yaw;
    }

    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, index = 2)
    private float clientmod$freelookPitch(float pitch) {
        return Freelook.active ? Freelook.pitch : pitch;
    }
}
