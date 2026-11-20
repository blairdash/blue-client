package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nick Hider: scoreboard sidebar entries and entity name tags (both go through PlayerTeam.formatNameForTeam). */
@Mixin(PlayerTeam.class)
public class NickHiderNameMixin {
    @Inject(method = "formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private static void blue$nickName(Team team, Component name, CallbackInfoReturnable<MutableComponent> cir) {
        BlueMod m = VisualMods.active("nick_hider");
        if (m == null || cir.getReturnValue() == null) return;
        Component out = ((VisualMods.NickHiderAccess) m).apply(cir.getReturnValue(), 2);
        if (out != cir.getReturnValue()) cir.setReturnValue(out instanceof MutableComponent mc ? mc : out.copy());
    }
}