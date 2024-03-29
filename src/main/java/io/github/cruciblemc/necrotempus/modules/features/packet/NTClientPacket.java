package io.github.cruciblemc.necrotempus.modules.features.packet;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.Tags;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

public class NTClientPacket implements IMessage {

    public NTClientPacket() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // does nothing
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, NecroTempusClient.getInstance().toNBT());
    }

    public static class NecroTempusClient {

        private static NTClientPacket packet;
        static int delay = 0;
        static int times = 0;

        @SubscribeEvent
        public void playerServerConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
            packet = new NTClientPacket();
            delay = 10;
            NecroTempus.getInstance().getLogger().info("Saying HELLO to the server.");
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {

            if (packet != null && delay-- <= 0) {

                NecroTempus.DISPATCHER.sendToServer(packet);
                times++;

                if (times > 8) {
                    packet = null;
                    delay = 10;
                }

            }
        }


        private static NecroTempusClient instance;

        public static NecroTempusClient getInstance() {
            return instance != null ? instance : (instance = new NecroTempusClient());
        }

        public NBTTagCompound toNBT() {

            NBTTagCompound nbtTagCompound = new NBTTagCompound();

            nbtTagCompound.setString("version", Tags.VERSION);

            return nbtTagCompound;
        }

    }

}
