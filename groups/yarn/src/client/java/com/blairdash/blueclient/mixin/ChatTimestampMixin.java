package com.blairdash.blueclient.mixin;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Chat Timestamps: puts a grey [HH:mm] in front of every chat line as it is added. */
@Mixin(ChatHud.class)
public class ChatTimestampMixin {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), argsOnly = true)
    private Text clientmod$timestamp(Text message) {
        if (!ModConfig.chatTimestampsEnabled) return message;
        var o = com.blairdash.blueclient.mods.LegacyOptions.holder("chat_opts");
        String pattern = (o.setting("twelve_hour").b ? "hh:mm" : "HH:mm") + (o.setting("seconds").b ? ":ss" : "")
                + (o.setting("twelve_hour").b ? " a" : "");
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern(pattern));
        int style = o.setting("brackets").idx;
        String stamp = style == 0 ? "[" + time + "] " : style == 1 ? "(" + time + ") " : time + " ";
        // Siblings under an empty root: the stamp's colour must not become the parent style of the message.
        return Text.empty()
                .append(Text.literal(stamp).styled(st -> st.withColor(o.setting("color").color & 0xFFFFFF)))
                .append(message);
    }
}