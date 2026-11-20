package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.features.Freelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While freelooking, mouse movement turns the camera instead of the player. */
@Mixin(Entity.class)
public class FreelookEntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void clientmod$freelook(double dx, double dy, CallbackInfo ci) {
        if (Freelook.active && (Object) this == MinecraftClient.getInstance().player) {
            Freelook.look(dx, dy);
            ci.cancel();
        }
    }
}
