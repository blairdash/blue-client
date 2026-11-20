package com.blairdash.blueclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

/** Swaps the running game's logged-in account without a relaunch (1.8.9 has no profile keys, only the session). */
public final class SessionSwap {
    private SessionSwap() { }

    /** Must be called on the render thread. */
    public static void apply(AccountManager.Session s) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        Session ns = new Session(s.username, s.uuid, s.accessToken, "mojang");
        ReflectionHelper.setPrivateValue(Minecraft.class, mc, ns, "session", "field_71449_j");
    }
}
