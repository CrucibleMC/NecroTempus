package io.github.crucible.necrotempus.modules.bossbar.internal.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.crucible.necrotempus.modules.bossbar.internal.manager.ClientBossBarManager;

public class BossBarPacketHandler implements IMessageHandler<BossBarPacket, IMessage> {

    @Override
    public IMessage onMessage(BossBarPacket message, MessageContext ctx) {
        handleBossBar(message);
        return null;
    }

    @SideOnly(Side.CLIENT)
    public static void handleBossBar(BossBarPacket bossBarPacket){

        switch (bossBarPacket.getPacketType()){

            case ADD:
            case UPDATE:{
                ClientBossBarManager.add(bossBarPacket.getComponent());
                return;
            }

            case REMOVE:{
                ClientBossBarManager.remove(bossBarPacket.getComponent());
            }

        }
    }
}
