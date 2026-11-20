package com.blairdash.blueclient.mixin;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sets the client brand (shown on the F3 screen, sent to servers in the minecraft:brand
 * packet and written into crash reports) to "Blue Client <loader>", for example
 * "Blue Client Fabric". The loader is detected at runtime, so the one source covers
 * Fabric, Quilt, Forge and NeoForge. A high priority makes this run after the loader's
 * own brand injection, so ours is the value that is returned.
 */
@Mixin(value = ClientBrandRetriever.class, priority = 2000)
public class ClientBrandMixin {

    private static String clientmod$brand;

    @Inject(method = "getClientModName", at = @At("RETURN"), cancellable = true, remap = false)
    private static void clientmod$brand(CallbackInfoReturnable<String> cir) {
        String b = clientmod$brand;
        if (b == null) {
            String[][] probes = {
                {"org.quiltmc.loader.api.QuiltLoader", "Quilt"},
                {"net.neoforged.fml.loading.FMLLoader", "NeoForge"},
                {"net.minecraftforge.fml.loading.FMLLoader", "Forge"},
                {"net.fabricmc.loader.api.FabricLoader", "Fabric"}
            };
            String loader = null;
            for (String[] p : probes) {
                try {
                    Class.forName(p[0], false, ClientBrandRetriever.class.getClassLoader());
                    loader = p[1];
                    break;
                } catch (Throwable ignored) {
                    // not this loader
                }
            }
            if (loader == null) {
                // Fall back to what the loader itself reported, e.g. "fabric" or "neoforge".
                String orig = String.valueOf(cir.getReturnValue()).toLowerCase(java.util.Locale.ROOT);
                if (orig.contains("neoforge")) loader = "NeoForge";
                else if (orig.contains("quilt")) loader = "Quilt";
                else if (orig.contains("forge")) loader = "Forge";
                else if (orig.contains("fabric")) loader = "Fabric";
            }
            b = loader == null ? "Blue Client" : "Blue Client " + loader;
            clientmod$brand = b;
        }
        cir.setReturnValue(b);
    }
}