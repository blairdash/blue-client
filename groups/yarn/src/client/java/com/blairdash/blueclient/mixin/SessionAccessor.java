package com.blairdash.blueclient.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface SessionAccessor {
    @Mutable @Accessor("session") void blue$setSession(Session session);
    @Mutable @Accessor("userApiService") void blue$setUserApiService(UserApiService service);
    @Mutable @Accessor("profileKeys") void blue$setProfileKeys(ProfileKeys keys);
}
