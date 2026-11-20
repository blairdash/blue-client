package com.blairdash.blueclient.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface SessionAccessor {
    @Mutable @Accessor("user") void blue$setUser(User user);
    @Mutable @Accessor("userApiService") void blue$setUserApiService(UserApiService service);
    @Mutable @Accessor("profileKeyPairManager") void blue$setProfileKeyPairManager(ProfileKeyPairManager manager);
}
