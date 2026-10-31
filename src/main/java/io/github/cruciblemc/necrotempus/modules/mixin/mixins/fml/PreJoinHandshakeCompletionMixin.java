package io.github.cruciblemc.necrotempus.modules.mixin.mixins.fml;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.common.network.internal.FMLMessage.CompleteHandshake;
import cpw.mods.fml.common.network.internal.HandshakeCompletionHandler;
import io.github.cruciblemc.necrotempus.modules.features.dialog.server.PreJoinServerAdapter;
import io.netty.channel.ChannelHandlerContext;

@Mixin(value = HandshakeCompletionHandler.class, remap = false)
public abstract class PreJoinHandshakeCompletionMixin {

    @Inject(
        method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lcpw/mods/fml/common/network/internal/FMLMessage$CompleteHandshake;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1,
        remap = false)
    private void necrotempus$suspendPreJoin(ChannelHandlerContext context, CompleteHandshake message, CallbackInfo ci) {
        if (PreJoinServerAdapter
            .suspend(context, ((CompleteHandshakeAccessor) (Object) message).necrotempus$getTarget())) ci.cancel();
    }
}
