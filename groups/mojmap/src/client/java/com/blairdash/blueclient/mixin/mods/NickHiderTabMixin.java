package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nick Hider: tab list entries. */
@Mixin(PlayerTabOverlay.class)
public class NickHiderTabMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true, require = 0)
    private void blue$nickTab(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        BlueMod m = VisualMods.active("nick_hider");
        if (m != null && cir.getReturnValue() != null) cir.setReturnValue(((VisualMods.NickHiderAccess) m).apply(cir.getReturnValue(), 1));
    }
}