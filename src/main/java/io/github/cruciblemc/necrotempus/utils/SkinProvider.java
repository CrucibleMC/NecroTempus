package io.github.cruciblemc.necrotempus.utils;

import com.mojang.authlib.GameProfile;
import net.minecraft.util.ResourceLocation;

@FunctionalInterface
public interface SkinProvider {

    ResourceLocation getSkin(GameProfile profile);

}
