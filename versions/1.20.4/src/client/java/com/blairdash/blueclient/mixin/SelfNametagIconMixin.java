package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.net.RankService;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Rank icon in front of a Blue Client player's nametag. Before 1.21 the name text comes from
 * Entity#getDisplayName inside EntityRenderer#render (there is no EntityRenderer#getDisplayName), so the
 * Text argument of the renderLabelIfPresent call is rewritten instead.
 */
@Mixin(EntityRenderer.class)
public abstract class SelfNametagIconMixin {

    @ModifyArgs(method = "render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderer;renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void blueclient$prependRankStar(Args args) {
        Entity entity = args.get(0);
        args.set(1, com.blairdash.blueclient.gui.NametagIcon.decorate(entity, args.get(1)));
    }
}
