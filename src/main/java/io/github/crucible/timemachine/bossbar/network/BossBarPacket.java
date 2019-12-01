package io.github.crucible.timemachine.bossbar.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.github.crucible.timemachine.bossbar.BossBarComponent;
import io.github.crucible.timemachine.bossbar.server.BossBar;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;


public class BossBarPacket extends Packet implements IMessage {

    private BossBarComponent component;
    private PacketType packetType;

    public BossBarPacket(NBTTagCompound tagCompound, PacketType type){
        component = new BossBar(tagCompound);
        packetType = type;
    }

    public BossBarPacket(BossBarComponent bossBarComponent, PacketType type){
        component = bossBarComponent;
        packetType = type;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NBTTagCompound tagCompound = ByteBufUtils.readTag(buf);
        packetType = PacketType.valueOf(tagCompound.getString("packetType"));
        component = new BossBar(tagCompound);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound tagCompound = component.toNbt();
        tagCompound.setString("packetType", packetType.getValue());
        ByteBufUtils.writeTag(buf, tagCompound);
    }

    @Override
    public void readPacketData(PacketBuffer packetBuffer) {
        NBTTagCompound tagCompound = ByteBufUtils.readTag(packetBuffer);
        packetType = PacketType.valueOf(tagCompound.getString("packetType"));
        component = new BossBar(tagCompound);
    }

    @Override
    public void writePacketData(PacketBuffer packetBuffer) {
        NBTTagCompound tagCompound = component.toNbt();
        tagCompound.setString("packetType", packetType.getValue());
        ByteBufUtils.writeTag(packetBuffer, tagCompound);
    }

    @Override
    public void processPacket(INetHandler iNetHandler) {
        BossBarPacketHandler.handleBossBar(this);
    }

    public enum PacketType{
        ADD("add"),REMOVE("remove"),UPDATE("update");

        private String value;

        PacketType(String value){
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    public BossBarComponent getComponent() {
        return component;
    }

    public PacketType getPacketType() {
        return packetType;
    }
}
