package com.blairdash.blueclient.mixin.mods;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.entity.player.HungerManager.class)
public interface HudHungerAccessor {
    @Accessor("exhaustion") float blueclient$exhaustion();
}
