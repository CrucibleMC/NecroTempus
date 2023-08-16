package io.github.cruciblemc.necrotempus.modules.features.packet;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.Tags;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;

public class NTClientPacket extends Packet implements IMessage {

    public NTClientPacket(){}

    @Override
    public void fromBytes(ByteBuf buf) {
        // does nothing
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, NecroTempusClient.getInstance().toNBT());
    }

    @Override
    public void readPacketData(PacketBuffer packetBuffer) throws IOException {
        // does nothing
    }

    @Override
    public void writePacketData(PacketBuffer packetBuffer) throws IOException {
        ByteBufUtils.writeTag(packetBuffer, NecroTempusClient.getInstance().toNBT());
    }

    @Override
    public void processPacket(INetHandler handler) {
        // does nothing
    }

    public static class NecroTempusClient {

        @SubscribeEvent
        public final void playerServerConnect(FMLNetworkEvent.ClientConnectedToServerEvent event){

            if(NecroTempus.DISPATCHER == null)
                return;

            NecroTempus.DISPATCHER.sendToServer(new NTClientPacket());
            NecroTempus.getInstance().getLogger().info("Sending a Hello to server...");
        }


        private static NecroTempusClient instance;

        public static NecroTempusClient getInstance(){
            return instance != null ? instance : (instance = new NecroTempusClient());
        }

        public NBTTagCompound toNBT(){

            NBTTagCompound nbtTagCompound = new NBTTagCompound();

            nbtTagCompound.setString("version", Tags.VERSION);

            return nbtTagCompound;
        }

    }

}
