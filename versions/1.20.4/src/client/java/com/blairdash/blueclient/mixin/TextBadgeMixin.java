package com.blairdash.blueclient.mixin;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;

/** Intentionally empty on this version: TextDrawerBadgeMixin draws the badge for GUI text as well. */
@Mixin(DrawContext.class)
public abstract class TextBadgeMixin { }