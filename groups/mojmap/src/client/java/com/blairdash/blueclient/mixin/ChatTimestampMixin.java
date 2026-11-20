package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Chat Timestamps: puts a grey [HH:mm] in front of every chat line as it is added. */
@Mixin(ChatComponent.class)
public class ChatTimestampMixin {

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true)
    private Component clientmod$timestamp(Component message) {
        if (!ModConfig.chatTimestampsEnabled) return message;
        var o = com.blairdash.blueclient.mods.LegacyOptions.holder("chat_opts");
        String pattern = (o.setting("twelve_hour").b ? "hh:mm" : "HH:mm") + (o.setting("seconds").b ? ":ss" : "")
                + (o.setting("twelve_hour").b ? " a" : "");
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern(pattern));
        int style = o.setting("brackets").idx;
        String stamp = style == 0 ? "[" + time + "] " : style == 1 ? "(" + time + ") " : time + " ";
        // Siblings under an empty root: the stamp's colour must not become the parent style of the message.
        return Component.empty()
                .append(Component.literal(stamp).withStyle(st -> st.withColor(o.setting("color").color & 0xFFFFFF)))
                .append(message);
    }
}
