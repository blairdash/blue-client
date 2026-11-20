package com.blairdash.blueclient.mixin.mods;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface HudCooldownEntryAccessor {
    @Accessor("endTick") int blueclient$endTick();
}
