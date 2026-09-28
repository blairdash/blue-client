package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class SelfNametagIconMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void clientmod$addIconToOwnName(Entity entity, CallbackInfoReturnable<Text> cir) {
        if (!ModConfig.selfNametagEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity == client.player && cir.getReturnValue() != null) {
            // The name is appended to the star, so it inherits the star's color.
            Text withIcon = Text.literal("\u2605 ").formatted(Formatting.BLUE)
                    .append(cir.getReturnValue());
            cir.setReturnValue(withIcon);
        }
    }
}