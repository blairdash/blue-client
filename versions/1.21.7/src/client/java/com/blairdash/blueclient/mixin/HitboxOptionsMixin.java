package com.blairdash.blueclient.mixin;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 1.21.7 renders hitboxes from a prebuilt EntityHitbox state (no Entity available), so the custom
 * hitbox options (colors, eye line, look vector, thickness) are not supported here; vanilla F3+B still works.
 */
@Mixin(EntityRenderDispatcher.class)
public class HitboxOptionsMixin {
}