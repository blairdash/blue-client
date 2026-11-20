package com.blairdash.blueclient.mixin.mods;

import com.blairdash.blueclient.mods.TipLines;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Turns Blue Client's marker lines (food icons, shulker grid) into real tooltip components. */
@Mixin(ClientTooltipComponent.class)
public interface TooltipMarkerMixin extends ClientTooltipComponent {
    @Inject(at = @At("HEAD"),
            method = "create(Lnet/minecraft/util/FormattedCharSequence;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
            cancellable = true)
    private static void blueclient$create(FormattedCharSequence text, CallbackInfoReturnable<ClientTooltipComponent> info) {
        ClientTooltipComponent c = TipLines.resolve(text);
        if (c != null) info.setReturnValue(c);
    }
}
