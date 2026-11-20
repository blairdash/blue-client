package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swaps the local player's cape for a Blue Client cosmetic cape. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class CapeMixin {
    private static final AssetInfo.TextureAssetInfo LIGHT = new AssetInfo.TextureAssetInfo(Identifier.of("clientmod", "cape/cape_light"));
    private static final AssetInfo.TextureAssetInfo BLUE = new AssetInfo.TextureAssetInfo(Identifier.of("clientmod", "cape/cape_blue"));
    private static final AssetInfo.TextureAssetInfo DARK = new AssetInfo.TextureAssetInfo(Identifier.of("clientmod", "cape/cape_dark"));

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void clientmod$cape(CallbackInfoReturnable<SkinTextures> cir) {
        BlueMod m = VisualMods.active("cosmetics");
        if (m == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || (Object) this != mc.player) return;
        int i = m.setting("cape").idx;
        if (i == 0) return;
        SkinTextures s = cir.getReturnValue();
        AssetInfo.TextureAsset cape = i == 1 ? LIGHT : i == 2 ? DARK : BLUE;
        cir.setReturnValue(new SkinTextures(s.body(), cape, s.elytra(), s.model(), s.secure()));
    }
}
