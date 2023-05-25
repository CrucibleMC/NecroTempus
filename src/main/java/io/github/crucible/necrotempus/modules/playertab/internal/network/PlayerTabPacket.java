package io.github.crucible.necrotempus.modules.playertab.internal.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.github.crucible.necrotempus.modules.playertab.internal.component.PlayerTab;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

public class PlayerTabPacket extends Packet implements IMessage {

    private PlayerTab component;
    private PlayerTabPacket.PacketType packetType = PacketType.SET;

    public PlayerTabPacket(){}

    public PlayerTabPacket(NBTTagCompound tagCompound, PacketType type){
        component = PlayerTab.fromCompound(tagCompound);
        packetType = type;
    }

    public PlayerTabPacket(PlayerTab playerTab, PacketType type){
        component = playerTab;
        packetType = type;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NBTTagCompound tagCompound = ByteBufUtils.readTag(buf);
        packetType = PacketType.valueOfString(tagCompound.getString("packetType"));
        component = PlayerTab.fromCompound(tagCompound);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound tagCompound = component.toNbt();
        tagCompound.setString("packetType", packetType.getName());
        ByteBufUtils.writeTag(buf, tagCompound);
    }

    @Override
    public void readPacketData(PacketBuffer packetBuffer) {
        NBTTagCompound tagCompound = ByteBufUtils.readTag(packetBuffer);
        packetType = PacketType.valueOfString(tagCompound.getString("packetType"));
        component = PlayerTab.fromCompound(tagCompound);
    }

    @Override
    public void writePacketData(PacketBuffer packetBuffer) {
        NBTTagCompound tagCompound = component.toNbt();
        tagCompound.setString("packetType", packetType.getName());
        ByteBufUtils.writeTag(packetBuffer, tagCompound);
    }

    @Override
    public void processPacket(INetHandler iNetHandler) {
        PlayerTabPacketHandler.handleBossBar(this);
    }

    public enum PacketType{

        SET("set"),
        UPDATE("update"),
        REMOVE("remove");

        private final String name;

        PacketType(String name){
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static PacketType valueOfString(String name){
            for(PacketType type : values()){
                if(type.getName().equalsIgnoreCase(name)){
                    return type;
                }
            }
            return SET;
        }

    }

    public PlayerTab getComponent() {
        return component;
    }

    public PacketType getPacketType() {
        return packetType;
    }

}
