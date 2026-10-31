package io.github.cruciblemc.necrotempus.modules.features.dialog.prejoin;

import net.minecraft.client.network.NetHandlerPlayClient;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.modules.features.dialog.client.PreJoinClientConnection;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.PreJoinPacket;

@SideOnly(Side.CLIENT)
public final class PreJoinClientPacketHandler implements IMessageHandler<PreJoinPacket, IMessage> {

    @Override
    public IMessage onMessage(PreJoinPacket message, MessageContext context) {
        if (context.side != Side.CLIENT || !message.isValidFor(Side.CLIENT)) return null;
        NetHandlerPlayClient handler = context.getClientHandler();
        if (handler != null) PreJoinClientConnection.receive(message, handler);
        return null;
    }
}
