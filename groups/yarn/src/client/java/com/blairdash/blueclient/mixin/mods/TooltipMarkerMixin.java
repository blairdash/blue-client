package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.TipLines;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Turns Blue Client's marker lines (food icons, shulker grid) into real tooltip components. */
@Mixin(TooltipComponent.class)
public interface TooltipMarkerMixin extends TooltipComponent {
    @Inject(at = @At("HEAD"),
            method = "of(Lnet/minecraft/text/OrderedText;)Lnet/minecraft/client/gui/tooltip/TooltipComponent;",
            cancellable = true)
    private static void blueclient$of(OrderedText text, CallbackInfoReturnable<TooltipComponent> info) {
        TooltipComponent c = TipLines.resolve(text);
        if (c != null) info.setReturnValue(c);
    }
}
