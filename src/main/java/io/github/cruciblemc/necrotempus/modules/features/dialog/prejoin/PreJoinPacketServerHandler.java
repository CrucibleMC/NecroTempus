package io.github.cruciblemc.necrotempus.modules.features.dialog.prejoin;

import net.minecraft.network.NetHandlerPlayServer;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.PreJoinPacket;
import io.github.cruciblemc.necrotempus.modules.features.dialog.server.PreJoinServerAdapter;

public final class PreJoinPacketServerHandler implements IMessageHandler<PreJoinPacket, IMessage> {

    @Override
    public IMessage onMessage(PreJoinPacket message, MessageContext context) {
        if (context.side != Side.SERVER) return null;
        NetHandlerPlayServer handler = context.getServerHandler();
        if (handler != null) PreJoinServerAdapter.handle(message, handler.netManager.channel());
        return null;
    }
}
