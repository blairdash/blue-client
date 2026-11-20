package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fullbright: swaps the gamma (brightness) option value read by the lightmap; the real option is never touched. */
@Mixin(LightmapRenderStateExtractor.class)
public class FullbrightMixin {

    @ModifyExpressionValue(method = "extract",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                    ordinal = 1))
    private Object clientmod$fullbright(Object original) {
        return ModConfig.fullbrightEnabled ? (Object) Double.valueOf(com.blairdash.blueclient.mods.LegacyOptions.s("fullbright_opts", "brightness").n) : original;
    }
}
