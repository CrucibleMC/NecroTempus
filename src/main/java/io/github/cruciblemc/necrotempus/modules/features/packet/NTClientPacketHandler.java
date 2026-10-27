package io.github.cruciblemc.necrotempus.modules.features.packet;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.network.NetHandlerPlayServer;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class NTClientPacketHandler implements IMessageHandler<NTClientPacket, IMessage> {

    private static final Map<NetHandlerPlayServer, Integer> CHAT_HEADS_PROTOCOLS = Collections
        .synchronizedMap(new WeakHashMap<>());

    @Override
    public IMessage onMessage(NTClientPacket message, MessageContext ctx) {
        if (ctx.side == Side.SERVER) {
            if (message.getChatHeadsProtocol() == NTClientPacket.CHAT_HEADS_PROTOCOL) {
                CHAT_HEADS_PROTOCOLS.put(ctx.getServerHandler(), message.getChatHeadsProtocol());
            } else {
                CHAT_HEADS_PROTOCOLS.remove(ctx.getServerHandler());
            }
        }
        return null;
    }

    public static boolean supportsChatHeads(NetHandlerPlayServer handler) {
        return Integer.valueOf(NTClientPacket.CHAT_HEADS_PROTOCOL)
            .equals(CHAT_HEADS_PROTOCOLS.get(handler));
    }

}
