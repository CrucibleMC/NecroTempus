package io.github.cruciblemc.necrotempus.modules.features.playertab.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.modules.features.playertab.client.ClientPlayerTabManager;

public class PlayerTabPacketHandler implements IMessageHandler<PlayerTabPacket, IMessage> {

    @Override
    public IMessage onMessage(PlayerTabPacket message, MessageContext ctx) {
        handleBossBar(message);
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static void handleBossBar(PlayerTabPacket playerTabPacket) {
        if (playerTabPacket.getPacketType() == PlayerTabPacket.PacketType.REMOVE) {
            ClientPlayerTabManager.setPlayerTab(null);
        } else {
            ClientPlayerTabManager.setPlayerTab(playerTabPacket.getComponent());
        }
    }

}
