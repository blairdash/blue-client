package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Time Changer: the client clock answers with the configured time of day. Client-side only. */
@Mixin(targets = "net.minecraft.client.ClientClockManager$ClientClockInstance")
public class TimeChangerMixin {

    @Inject(method = "totalTicks", at = @At("HEAD"), cancellable = true)
    private void clientmod$fixedTime(CallbackInfoReturnable<Long> cir) {
        if (ModConfig.timeChangerEnabled) {
            cir.setReturnValue(com.blairdash.blueclient.features.WorldTweaks.timeOfDay());
        }
    }
}