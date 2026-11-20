package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class TeamViewMixin {
    @Inject(method = "hasOutline", at = @At("RETURN"), cancellable = true, require = 0)
    private void blue$teamView(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        BlueMod m = VisualMods.active("team_view");
        if (m != null && ((com.blairdash.blueclient.mods.VisualMods.TeamViewAccess) m).glows((MinecraftClient) (Object) this, entity)) cir.setReturnValue(true);
    }
}
