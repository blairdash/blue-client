package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.features.Freelook;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While freelooking, mouse movement turns the camera instead of the player. */
@Mixin(Entity.class)
public class FreelookEntityMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void clientmod$freelook(double dx, double dy, CallbackInfo ci) {
        if (Freelook.active && (Object) this == Minecraft.getInstance().player) {
            Freelook.look(dx, dy);
            ci.cancel();
        }
    }
}
