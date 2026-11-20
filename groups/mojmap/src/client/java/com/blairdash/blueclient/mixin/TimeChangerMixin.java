package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Time Changer: the client clock manager answers with the configured client-side time. */
@Mixin(ClientClockManager.class)
public class TimeChangerMixin {

    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    private void clientmod$fixedTime(Holder<WorldClock> clock, CallbackInfoReturnable<Long> cir) {
        if (ModConfig.timeChangerEnabled) {
            cir.setReturnValue(com.blairdash.blueclient.features.WorldTweaks.timeOfDay());
        }
    }
}
