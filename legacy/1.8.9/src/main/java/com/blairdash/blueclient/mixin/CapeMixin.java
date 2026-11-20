package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.features.ExtraMods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swaps the local player's cape for a Blue Client cosmetic cape. */
@Mixin(AbstractClientPlayer.class)
public abstract class CapeMixin {
    private static final ResourceLocation[] CAPES = {
        null,
        new ResourceLocation("blueclient", "textures/cape/cape_light.png"),
        new ResourceLocation("blueclient", "textures/cape/cape_dark.png"),
        new ResourceLocation("blueclient", "textures/cape/cape_blue.png")
    };

    @Inject(method = "getLocationCape", at = @At("RETURN"), cancellable = true)
    private void blueclient$cape(CallbackInfoReturnable<ResourceLocation> cir) {
        ExtraMods.Mod m = ExtraMods.cosmetics();
        if (m == null || (Object) this != Minecraft.getMinecraft().thePlayer) return;
        int i = m.mode();
        if (i > 0) cir.setReturnValue(CAPES[i]);
    }
}
