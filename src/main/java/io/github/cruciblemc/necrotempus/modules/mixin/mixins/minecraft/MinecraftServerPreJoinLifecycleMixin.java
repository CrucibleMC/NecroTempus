package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.cruciblemc.necrotempus.modules.features.dialog.server.PreJoinServerLifecycle;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerPreJoinLifecycleMixin {

    @Inject(method = "run()V", at = @At("HEAD"))
    private void necrotempus$bindPreJoinServerThread(CallbackInfo callback) {
        PreJoinServerLifecycle.serverStarting((MinecraftServer) (Object) this);
    }

    @Inject(method = "run()V", at = @At("RETURN"))
    private void necrotempus$clearPreJoinServerThread(CallbackInfo callback) {
        PreJoinServerLifecycle.serverStopped();
    }
}
