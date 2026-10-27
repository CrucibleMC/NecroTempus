package io.github.cruciblemc.necrotempus.modules.features.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S02PacketChat;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.modules.mixin.accessors.S02PacketChatSender;

public class ChatHeadPacketHandler implements IMessageHandler<ChatHeadPacket, IMessage> {

    @Override
    public IMessage onMessage(ChatHeadPacket message, MessageContext context) {
        if (!message.isValid() || context.side != Side.CLIENT) return null;
        handleClient(message);
        return null;
    }

    @SideOnly(Side.CLIENT)
    private static void handleClient(ChatHeadPacket message) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                NetHandlerPlayClient handler = Minecraft.getMinecraft()
                    .getNetHandler();
                if (handler == null) return;

                S02PacketChat packet = new S02PacketChat(message.getComponent(), message.isChat());
                S02PacketChatSender metadata = (S02PacketChatSender) packet;
                metadata.necrotempus$setSenderUuid(message.getSenderUuid());
                metadata.necrotempus$setSenderName(message.getTargetName());
                metadata.necrotempus$setSenderDisplayName(message.getDisplayName());
                handler.handleChat(packet);
            });
    }
}
