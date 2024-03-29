package io.github.cruciblemc.necrotempus.modules.features.title.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.modules.features.title.client.ClientTitleManager;
import io.github.cruciblemc.necrotempus.modules.features.title.component.TimedTitle;

public class TitlePacketHandler implements IMessageHandler<TitlePacket, IMessage> {

    @Override
    public IMessage onMessage(TitlePacket message, MessageContext ctx) {
        handleTitle(message);
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static void handleTitle(TitlePacket titlePacket) {
        if (titlePacket.getPacketType() == TitlePacket.PacketType.REMOVE) {
            ClientTitleManager.setCurrentTitle(null);
        } else {
            ClientTitleManager.setCurrentTitle(
                    TimedTitle.fromCompound(
                            titlePacket.getComponent().toNbt())
            );
        }
    }

}
