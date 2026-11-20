package com.blairdash.blueclient.features;

import net.minecraft.client.MinecraftClient;

/** Isolates the server-brand API that moved after Minecraft 1.20.1. */
final class MovementServerBrand {
    private MovementServerBrand() { }

    static String get(MinecraftClient c) {
        var network = c.getNetworkHandler();
        return network == null ? null : network.getBrand();
    }
}
