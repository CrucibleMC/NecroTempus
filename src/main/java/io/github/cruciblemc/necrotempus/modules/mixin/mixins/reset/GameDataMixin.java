package io.github.cruciblemc.necrotempus.modules.mixin.mixins.reset;

import cpw.mods.fml.common.registry.GameData;
import io.github.cruciblemc.necrotempus.modules.features.core.ClientResetState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameData.class, remap = false)
public class GameDataMixin {

    @Inject(method = "Lcpw/mods/fml/common/registry/GameData;revertToFrozen()V", at = @At("HEAD"))
    private static void revertToFrozen(CallbackInfo callbackInfo) {
        ClientResetState.resetRender();
    }

}
