package io.github.cruciblemc.necrotempus.modules.features.title.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.github.cruciblemc.necrotempus.api.title.TitleComponent;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

public class TitlePacket extends Packet implements IMessage {

    private TitleComponent component;
    private PacketType packetType = PacketType.SET;

    public TitlePacket(){}

    public TitlePacket(NBTTagCompound tagCompound, PacketType type){
        component = TitleComponent.fromCompound(tagCompound);
        packetType = type;
    }

    public TitlePacket(TitleComponent title, PacketType type){
        component = title;
        packetType = type;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NBTTagCompound tagCompound = ByteBufUtils.readTag(buf);
        packetType = PacketType.valueOfString(tagCompound.getString("packetType"));
        component = TitleComponent.fromCompound(tagCompound);
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
        component = TitleComponent.fromCompound(tagCompound);
    }

    @Override
    public void writePacketData(PacketBuffer packetBuffer) {
        NBTTagCompound tagCompound = component.toNbt();
        tagCompound.setString("packetType", packetType.getName());
        ByteBufUtils.writeTag(packetBuffer, tagCompound);
    }

    @Override
    public void processPacket(INetHandler iNetHandler) {
        TitlePacketHandler.handleTitle(this);
    }

    public enum PacketType{

        SET("set"),
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

    public TitleComponent getComponent() {
        return component;
    }

    public PacketType getPacketType() {
        return packetType;
    }

}
