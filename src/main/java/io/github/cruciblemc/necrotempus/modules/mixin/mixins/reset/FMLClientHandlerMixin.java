package io.github.cruciblemc.necrotempus.modules.mixin.mixins.reset;

import io.github.cruciblemc.necrotempus.modules.features.core.ClientResetState;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = cpw.mods.fml.client.FMLClientHandler.class, remap = false)
public class FMLClientHandlerMixin {

    @Inject(method = "Lcpw/mods/fml/client/FMLClientHandler;handleClientWorldClosing(Lnet/minecraft/client/multiplayer/WorldClient;)V", at = @At(value = "INVOKE", target = "Lcpw/mods/fml/common/registry/GameData;revertToFrozen()V"))
    public void frozen(WorldClient worldClient, CallbackInfo info) {
        ClientResetState.resetRender();
    }


}

