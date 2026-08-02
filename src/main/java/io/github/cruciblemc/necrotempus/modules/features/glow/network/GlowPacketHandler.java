package io.github.cruciblemc.necrotempus.modules.features.glow.network;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.modules.features.glow.GlowingEntityRegistry;
import io.github.cruciblemc.necrotempus.modules.features.glow.client.GlowClientManager;

public class GlowPacketHandler implements IMessageHandler<GlowPacket, IMessage> {

    @Override
    public IMessage onMessage(GlowPacket message, MessageContext ctx) {
        handle(message);
        return null;
    }

    @SideOnly(Side.CLIENT)
    private static void handle(GlowPacket msg) {
        if (!NecroTempusConfig.enableEntityGlow) return;
        // onMessage runs on the netty thread; mutate registry on the client thread.
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                GlowingEntityRegistry registry = GlowClientManager.getInstance()
                    .registry();
                switch (msg.getOp()) {
                    case SET:
                        registry.set(msg.getEntityId(), msg.getRgb(), msg.getDurationTicks());
                        break;
                    case REMOVE:
                        registry.remove(msg.getEntityId());
                        break;
                    case CLEAR:
                        registry.clear();
                        break;
                }
            });
    }
}
