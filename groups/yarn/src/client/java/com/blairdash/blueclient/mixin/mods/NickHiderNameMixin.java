package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nick Hider: scoreboard sidebar entries and entity name tags (both go through Team.decorateName). */
@Mixin(Team.class)
public class NickHiderNameMixin {
    @Inject(method = "decorateName(Lnet/minecraft/scoreboard/AbstractTeam;Lnet/minecraft/text/Text;)Lnet/minecraft/text/MutableText;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private static void blue$nickName(AbstractTeam team, Text name, CallbackInfoReturnable<MutableText> cir) {
        BlueMod m = VisualMods.active("nick_hider");
        if (m == null || cir.getReturnValue() == null) return;
        Text out = ((VisualMods.NickHiderAccess) m).apply(cir.getReturnValue(), 2);
        if (out != cir.getReturnValue()) cir.setReturnValue(out instanceof MutableText mt ? mt : out.copy());
    }
}