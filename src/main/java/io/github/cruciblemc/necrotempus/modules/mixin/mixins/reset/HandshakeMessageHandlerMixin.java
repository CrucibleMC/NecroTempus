package io.github.cruciblemc.necrotempus.modules.mixin.mixins.reset;

import cpw.mods.fml.common.network.handshake.FMLHandshakeMessage;
import cpw.mods.fml.common.network.handshake.HandshakeMessageHandler;
import cpw.mods.fml.common.network.handshake.IHandshakeState;
import io.github.cruciblemc.necrotempus.modules.features.core.ClientResetState;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HandshakeMessageHandler.class, remap = false)
public class HandshakeMessageHandlerMixin<S extends Enum<S> & IHandshakeState<S>> {

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lcpw/mods/fml/common/network/handshake/FMLHandshakeMessage;)V", at = @At("HEAD"))
    public void channelRead0(ChannelHandlerContext context, FMLHandshakeMessage fmlHandshakeMessage, CallbackInfo callbackInfo){
        if(fmlHandshakeMessage instanceof FMLHandshakeMessage.HandshakeReset){
            ClientResetState.resetRender();
        }
    }

}