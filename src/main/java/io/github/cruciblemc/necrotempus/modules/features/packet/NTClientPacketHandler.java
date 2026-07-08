package io.github.cruciblemc.necrotempus.modules.features.packet;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class NTClientPacketHandler implements IMessageHandler<NTClientPacket, IMessage> {

    @Override
    public IMessage onMessage(NTClientPacket message, MessageContext ctx) {
        return null;
    }

}
