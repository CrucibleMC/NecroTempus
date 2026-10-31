package io.github.cruciblemc.necrotempus.modules.mixin.mixins.fml;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import cpw.mods.fml.common.network.internal.FMLMessage;
import cpw.mods.fml.relauncher.Side;

@Mixin(value = FMLMessage.CompleteHandshake.class, remap = false)
public interface CompleteHandshakeAccessor {

    @Accessor("target")
    Side necrotempus$getTarget();
}
