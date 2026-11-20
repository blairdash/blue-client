package com.blairdash.blueclient.mixin.mods;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.world.item.ItemCooldowns.class)
public interface HudCooldownAccessor {
    @Accessor("cooldowns") java.util.Map<?, ?> blueclient$entries();
    @Accessor("tickCount") int blueclient$tick();
}
