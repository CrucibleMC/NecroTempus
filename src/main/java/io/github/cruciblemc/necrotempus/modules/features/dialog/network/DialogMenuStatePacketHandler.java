package io.github.cruciblemc.necrotempus.modules.features.dialog.network;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.modules.features.dialog.client.DialogMenuClientManager;

public final class DialogMenuStatePacketHandler implements IMessageHandler<DialogMenuStatePacket, IMessage> {

    @Override
    public IMessage onMessage(DialogMenuStatePacket message, MessageContext context) {
        if (!message.isValid() || context.side != Side.CLIENT) return null;
        handleClient(message);
        return null;
    }

    @SideOnly(Side.CLIENT)
    private static void handleClient(DialogMenuStatePacket message) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> DialogMenuClientManager.apply(message));
    }
}
