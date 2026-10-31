package io.github.cruciblemc.necrotempus.modules.features.dialog.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.github.cruciblemc.necrotempus.modules.features.dialog.server.DialogSessions;
import io.github.cruciblemc.necrotempus.modules.features.packet.NTClientPacketHandler;

public final class DialogResponsePacketHandler implements IMessageHandler<DialogResponsePacket, IMessage> {

    @Override
    public IMessage onMessage(DialogResponsePacket message, MessageContext context) {
        if (!message.isValid() || context.side != Side.SERVER
            || !NTClientPacketHandler.supportsDialogs(context.getServerHandler())) return null;
        DialogSessions.enqueue(context.getServerHandler().playerEntity, message);
        return null;
    }
}
