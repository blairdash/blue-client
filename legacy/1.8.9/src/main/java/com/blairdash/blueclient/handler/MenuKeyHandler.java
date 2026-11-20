package com.blairdash.blueclient.handler;

import com.blairdash.blueclient.BlueClientMod;
import com.blairdash.blueclient.gui.BlueClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Opens the mod menu overlay when Right Shift is pressed with no other screen open. */
public class MenuKeyHandler {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (BlueClientMod.openMenuKey != null && BlueClientMod.openMenuKey.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new BlueClientScreen());
        }
    }
}
