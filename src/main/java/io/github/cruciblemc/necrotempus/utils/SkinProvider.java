package io.github.cruciblemc.necrotempus.utils;

import net.minecraft.util.ResourceLocation;

import com.mojang.authlib.GameProfile;

@FunctionalInterface
public interface SkinProvider {

    ResourceLocation getSkin(GameProfile profile);

}
