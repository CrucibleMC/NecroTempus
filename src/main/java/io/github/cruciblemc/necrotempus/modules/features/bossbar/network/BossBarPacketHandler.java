package io.github.cruciblemc.necrotempus.modules.features.bossbar.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.ClientBossBarManager;

public class BossBarPacketHandler implements IMessageHandler<BossBarPacket, IMessage> {

    @Override
    public IMessage onMessage(BossBarPacket message, MessageContext ctx) {
        handleBossBar(message);
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static void handleBossBar(BossBarPacket bossBarPacket) {

        if (bossBarPacket.getPacketType() == BossBarPacket.PacketType.REMOVE)
            ClientBossBarManager.remove(bossBarPacket.getComponent());
        else
            ClientBossBarManager.add(bossBarPacket.getComponent());

    }
}
