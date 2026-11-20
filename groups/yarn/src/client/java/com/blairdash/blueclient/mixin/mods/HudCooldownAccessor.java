package com.blairdash.blueclient.mixin.mods;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.entity.player.ItemCooldownManager.class)
public interface HudCooldownAccessor {
    @Accessor("entries") java.util.Map<?, ?> blueclient$entries();
    @Accessor("tick") int blueclient$tick();
}
