package com.blairdash.blueclient.gui;

import com.blairdash.blueclient.mixin.SessionAccessor;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;

import java.util.Optional;
import java.util.UUID;

/** Swaps the running game's logged-in account (session + services that depend on it) without a relaunch. */
public final class SessionSwap {
    private SessionSwap() { }

    private static volatile com.mojang.authlib.GameProfile switched;

    /** Profile of the account switched to at runtime; the game's own getGameProfile() stays fixed at its startup value. */
    public static com.mojang.authlib.GameProfile profile(com.mojang.authlib.GameProfile fallback) { com.mojang.authlib.GameProfile p = switched; return p != null ? p : fallback; }

    static UUID toUuid(String raw) {
        return UUID.fromString(raw.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
    }

    /** Must be called on the render thread. */
    public static void apply(AccountManager.Session s) throws Exception {
        MinecraftClient mc = MinecraftClient.getInstance();
        Session ns = new Session(s.username, toUuid(s.uuid), s.accessToken, Optional.empty(), Optional.empty());
        UserApiService api = new YggdrasilAuthenticationService(mc.getNetworkProxy()).createUserApiService(s.accessToken);
        SessionAccessor acc = (SessionAccessor) (Object) mc;
        acc.blue$setSession(ns);
        acc.blue$setUserApiService(api);
        acc.blue$setProfileKeys(ProfileKeys.create(api, ns, mc.runDirectory.toPath()));
        switched = new com.mojang.authlib.GameProfile(toUuid(s.uuid), s.username);
    }
}
