package io.github.crucible.necrotempus.modules.features.actionbar.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.github.crucible.necrotempus.api.actionbar.ActionBar;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

public class ActionBarPacket extends Packet implements IMessage {

    private ActionBar component;
    private PacketType packetType = PacketType.SET;

    public ActionBarPacket(){}

    public ActionBarPacket(NBTTagCompound tagCompound, PacketType type){
        component = ActionBar.fromCompound(tagCompound);
        packetType = type;
    }

    public ActionBarPacket(ActionBar actionBar, PacketType type){
        component = actionBar;
        packetType = type;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NBTTagCompound tagCompound = ByteBufUtils.readTag(buf);
        packetType = PacketType.valueOfString(tagCompound.getString("packetType"));
        component = ActionBar.fromCompound(tagCompound);
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
        component = ActionBar.fromCompound(tagCompound);
    }

    @Override
    public void writePacketData(PacketBuffer packetBuffer) {
        NBTTagCompound tagCompound = component.toNbt();
        tagCompound.setString("packetType", packetType.getName());
        ByteBufUtils.writeTag(packetBuffer, tagCompound);
    }

    @Override
    public void processPacket(INetHandler iNetHandler) {
        ActionBarPacketHandler.handleActionBar(this);
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

    public ActionBar getComponent() {
        return component;
    }

    public PacketType getPacketType() {
        return packetType;
    }

}
