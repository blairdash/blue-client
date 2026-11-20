package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fullbright: the lightmap reads the brightness (gamma) option once per
 * update. It is the third SimpleOption.getValue() call in update()
 * (after hide-lightning-flashes and darkness-effect-scale), so we swap
 * only that value. The in-game option itself is never touched, so
 * turning Fullbright off restores the player's own brightness.
 */
@Mixin(LightmapTextureManager.class)
public class FullbrightMixin {

    @ModifyExpressionValue(method = "update(F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;",
                    ordinal = 2))
    private Object clientmod$fullbright(Object original) {
        return ModConfig.fullbrightEnabled ? (Object) Double.valueOf(com.blairdash.blueclient.mods.LegacyOptions.s("fullbright_opts", "brightness").n) : original;
    }
}
