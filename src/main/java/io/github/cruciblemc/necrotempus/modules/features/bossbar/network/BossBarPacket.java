package io.github.cruciblemc.necrotempus.modules.features.bossbar.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBar;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

public class BossBarPacket implements IMessage {

    private BossBar component;
    private PacketType packetType = PacketType.ADD;

    public BossBarPacket() {
    }

    public BossBarPacket(NBTTagCompound tagCompound, PacketType type) {
        component = BossBar.createBossBar(tagCompound);
        packetType = type;
    }

    public BossBarPacket(BossBar bossBarComponent, PacketType type) {
        component = bossBarComponent;
        packetType = type;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NBTTagCompound tagCompound = ByteBufUtils.readTag(buf);
        packetType = PacketType.valueOfString(tagCompound.getString("packetType"));
        component = BossBar.createBossBar(tagCompound);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        NBTTagCompound tagCompound = component.toNbt();
        tagCompound.setString("packetType", packetType.getName());
        ByteBufUtils.writeTag(buf, tagCompound);
    }

    public enum PacketType {

        ADD("add"),
        REMOVE("remove"),
        UPDATE("update");

        private final String name;

        PacketType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static PacketType valueOfString(String name) {
            for (PacketType type : values()) {
                if (type.getName().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return ADD;
        }

    }

    public BossBar getComponent() {
        return component;
    }

    public PacketType getPacketType() {
        return packetType;
    }
}
