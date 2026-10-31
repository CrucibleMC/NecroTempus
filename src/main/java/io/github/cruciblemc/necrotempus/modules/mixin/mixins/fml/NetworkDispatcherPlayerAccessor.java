package io.github.cruciblemc.necrotempus.modules.mixin.mixins.fml;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import cpw.mods.fml.common.network.handshake.NetworkDispatcher;

@Mixin(value = NetworkDispatcher.class, remap = false)
public interface NetworkDispatcherPlayerAccessor {

    @Accessor("player")
    EntityPlayerMP necrotempus$getPlayer();
}
