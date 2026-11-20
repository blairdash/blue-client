package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the Mob Size module (the module already exists in the menu; this is the hook that makes it do something). */
@Mixin(LivingEntityRenderer.class)
public class MobSizeMixin {
    @Inject(method = "scale", at = @At("HEAD"), require = 0)
    private void blue$mobSize(LivingEntity entity, MatrixStack matrices, float amount, CallbackInfo ci) {
        if (entity instanceof PlayerEntity) return;
        BlueMod m = VisualMods.active("mob_size");
        if (m != null) { float s = (float) m.setting("scale").n; matrices.scale(s, s, s); }
    }
}