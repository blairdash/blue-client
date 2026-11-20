package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swaps the local player's cape for a Blue Client cosmetic cape. */
@Mixin(AbstractClientPlayer.class)
public abstract class CapeMixin {
    private static final ClientAsset.ResourceTexture LIGHT = new ClientAsset.ResourceTexture(Identifier.fromNamespaceAndPath("clientmod", "cape/cape_light"));
    private static final ClientAsset.ResourceTexture BLUE = new ClientAsset.ResourceTexture(Identifier.fromNamespaceAndPath("clientmod", "cape/cape_blue"));
    private static final ClientAsset.ResourceTexture DARK = new ClientAsset.ResourceTexture(Identifier.fromNamespaceAndPath("clientmod", "cape/cape_dark"));

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void clientmod$cape(CallbackInfoReturnable<PlayerSkin> cir) {
        BlueMod m = VisualMods.active("cosmetics");
        if (m == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || (Object) this != mc.player) return;
        int i = m.setting("cape").idx;
        if (i == 0) return;
        PlayerSkin s = cir.getReturnValue();
        ClientAsset.Texture cape = i == 1 ? LIGHT : i == 2 ? DARK : BLUE;
        cir.setReturnValue(new PlayerSkin(s.body(), cape, s.elytra(), s.model(), s.secure()));
    }
}
