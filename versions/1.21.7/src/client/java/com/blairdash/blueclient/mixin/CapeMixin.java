package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swaps the local player's cape for a Blue Client cosmetic cape. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class CapeMixin {
    private static final Identifier LIGHT = Identifier.of("clientmod", "textures/cape/cape_light.png");
    private static final Identifier BLUE = Identifier.of("clientmod", "textures/cape/cape_blue.png");
    private static final Identifier DARK = Identifier.of("clientmod", "textures/cape/cape_dark.png");

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void clientmod$cape(CallbackInfoReturnable<SkinTextures> cir) {
        BlueMod m = VisualMods.active("cosmetics");
        if (m == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || (Object) this != mc.player) return;
        int i = m.setting("cape").idx;
        if (i == 0) return;
        SkinTextures s = cir.getReturnValue();
        Identifier cape = i == 1 ? LIGHT : i == 2 ? DARK : BLUE;
        cir.setReturnValue(new SkinTextures(s.texture(), s.textureUrl(), cape, s.elytraTexture(), s.model(), s.secure()));
    }
}
