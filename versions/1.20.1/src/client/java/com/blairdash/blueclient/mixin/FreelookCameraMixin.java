package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.features.Freelook;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Points the camera at the freelook angles instead of the player's own. */
@Mixin(Camera.class)
public class FreelookCameraMixin {
    @ModifyArgs(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void clientmod$freelookRotation(Args args) {
        if (Freelook.active) {
            args.set(0, Freelook.yaw);
            args.set(1, Freelook.pitch);
        }
    }
}
