package com.blairdash.blueclient.mixin;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes "EncoderException: Sending unknown packet 'serverbound/minecraft:client_tick_end'" when a proxy
 * (Velocity/BungeeCord) moves you to another server. If the switch to the configuration phase is handled
 * earlier in the same client tick, Minecraft.tick() still sends the play-phase tick-end packet and the
 * encoder disconnects. Drop that packet when this handler is no longer the connection's listener.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientTickEndMixin {
    @Shadow protected Connection connection;

    @Inject(method = "send", at = @At("HEAD"), cancellable = true)
    private void bluemod$dropStaleTickEnd(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ServerboundClientTickEndPacket && this.connection.getPacketListener() != (Object) this) {
            ci.cancel();
        }
    }
}