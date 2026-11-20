package com.blairdash.blueclient.mixin;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sets the client brand (F3 screen, minecraft:brand packet, crash reports) to "Blue Client Forge". */
@Mixin(value = ClientBrandRetriever.class, priority = 2000)
public class ClientBrandMixin {

    @Inject(method = "getClientModName", at = @At("RETURN"), cancellable = true, remap = false)
    private static void blueclient$brand(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Blue Client Forge");
    }
}