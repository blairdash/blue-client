package com.blairdash.blueclient.mixin.mods;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.item.ItemCooldowns$CooldownInstance")
public interface HudCooldownEntryAccessor {
    @Accessor("endTime") int blueclient$endTick();
}
