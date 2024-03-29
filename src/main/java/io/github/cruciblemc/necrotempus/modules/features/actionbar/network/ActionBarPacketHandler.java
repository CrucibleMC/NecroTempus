package io.github.cruciblemc.necrotempus.modules.features.actionbar.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.modules.features.actionbar.client.ClientGuiIngameModifier;

public class ActionBarPacketHandler implements IMessageHandler<ActionBarPacket, IMessage> {

    @Override
    public IMessage onMessage(ActionBarPacket message, MessageContext ctx) {
        handleActionBar(message);
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static void handleActionBar(ActionBarPacket actionBarPacket) {
        if (actionBarPacket.getPacketType() == ActionBarPacket.PacketType.REMOVE) {
            ClientGuiIngameModifier.clearActionbar();
        } else {
            ClientGuiIngameModifier.renderActionBar(actionBarPacket.getComponent());
        }
    }

}
