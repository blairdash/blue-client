package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.BlueMod;
import com.blairdash.blueclient.mods.VisualMods;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void blue$amount(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        if (options.getType() == net.minecraft.core.particles.ParticleTypes.FIREWORK) return; // vanilla casts this result, never cancel it
        BlueMod fb = VisualMods.active("fps_boost");
        if (fb != null && fb.setting("particle_dist").n > 0) {
            var pl = net.minecraft.client.Minecraft.getInstance().player;
            if (pl != null) {
                double d = fb.setting("particle_dist").n, dx = x - pl.getX(), dy = y - pl.getY(), dz = z - pl.getZ();
                if (dx * dx + dy * dy + dz * dz > d * d) { cir.setReturnValue(null); return; }
            }
        }
        BlueMod m = VisualMods.active("particle_changer");
        if (m != null && Math.random() * 100.0 >= m.setting("amount").n) cir.setReturnValue(null);
    }
    @Inject(method = "createParticle", at = @At("RETURN"))
    private void blue$scale(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        Particle particle = cir.getReturnValue();
        if (particle != null && VisualMods.active("particle_changer") != null) {
            particle.scale((float) com.blairdash.blueclient.mods.LegacyOptions.s("particle_opts", "scale").n);
        }
    }
}
