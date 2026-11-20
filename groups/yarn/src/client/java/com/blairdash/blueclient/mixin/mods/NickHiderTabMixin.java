package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nick Hider: tab list entries. */
@Mixin(PlayerListHud.class)
public class NickHiderTabMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true, require = 0)
    private void blue$nickTab(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        BlueMod m = VisualMods.active("nick_hider");
        if (m != null && cir.getReturnValue() != null) cir.setReturnValue(((VisualMods.NickHiderAccess) m).apply(cir.getReturnValue(), 1));
    }
}