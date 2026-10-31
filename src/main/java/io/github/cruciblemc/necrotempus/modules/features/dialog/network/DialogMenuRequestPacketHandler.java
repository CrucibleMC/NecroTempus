package io.github.cruciblemc.necrotempus.modules.features.dialog.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.github.cruciblemc.necrotempus.modules.features.dialog.server.DialogSessions;
import io.github.cruciblemc.necrotempus.modules.features.packet.NTClientPacketHandler;

public final class DialogMenuRequestPacketHandler implements IMessageHandler<DialogMenuRequestPacket, IMessage> {

    @Override
    public IMessage onMessage(DialogMenuRequestPacket message, MessageContext context) {
        if (!message.isValid() || context.side != Side.SERVER
            || !NTClientPacketHandler.supportsDialogs(context.getServerHandler())) return null;
        DialogSessions.enqueueMenu(context.getServerHandler().playerEntity, message);
        return null;
    }
}
