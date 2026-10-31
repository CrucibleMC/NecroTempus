package io.github.cruciblemc.necrotempus.modules.features.dialog.prejoin;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.PreJoinPacket;

public final class PreJoinNetwork {

    public static final String CHANNEL_NAME = "necrotempus:prejoin";
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);

    private PreJoinNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(PreJoinPacketServerHandler.class, PreJoinPacket.class, 1, Side.SERVER);
    }

    @SideOnly(Side.CLIENT)
    public static void registerClient() {
        CHANNEL.registerMessage(PreJoinClientPacketHandler.class, PreJoinPacket.class, 1, Side.CLIENT);
    }

    public static void send(NetworkDispatcher dispatcher, IMessage message) {
        dispatcher.sendProxy((FMLProxyPacket) CHANNEL.getPacketFrom(message));
    }

    public static net.minecraft.network.Packet getPacket(IMessage message) {
        return CHANNEL.getPacketFrom(message);
    }
}
