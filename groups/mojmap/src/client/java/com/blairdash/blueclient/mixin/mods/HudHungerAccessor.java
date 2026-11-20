package com.blairdash.blueclient.mixin.mods;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.world.food.FoodData.class)
public interface HudHungerAccessor {
    @Accessor("exhaustionLevel") float blueclient$exhaustion();
}
