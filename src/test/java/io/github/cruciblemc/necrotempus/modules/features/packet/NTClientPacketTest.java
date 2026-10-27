package io.github.cruciblemc.necrotempus.modules.features.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class NTClientPacketTest {

    @Test
    void currentClientAdvertisesChatHeadProtocol() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            new NTClientPacket().toBytes(buffer);
            NTClientPacket decoded = new NTClientPacket();
            decoded.fromBytes(buffer);
            assertEquals(2, decoded.getChatHeadsProtocol());
        } finally {
            buffer.release();
        }
    }

    @Test
    void missingCapabilityClearsPreviouslyAdvertisedProtocol() {
        NTClientPacket decoded = new NTClientPacket();
        NBTTagCompound current = new NBTTagCompound();
        current.setInteger("chatHeadsProtocol", 2);
        readTag(decoded, current);
        assertEquals(2, decoded.getChatHeadsProtocol());

        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setString("version", "legacy");
        readTag(decoded, legacy);
        assertEquals(0, decoded.getChatHeadsProtocol());
        readTag(decoded, null);
        assertEquals(0, decoded.getChatHeadsProtocol());
    }

    @Test
    void doesNotUpgradeOlderNewerOrInvalidProtocolValues() {
        NTClientPacket decoded = new NTClientPacket();
        for (int protocol : new int[] { 1, 3, -1, Integer.MAX_VALUE }) {
            NBTTagCompound data = new NBTTagCompound();
            data.setInteger("chatHeadsProtocol", protocol);
            readTag(decoded, data);
            assertEquals(protocol, decoded.getChatHeadsProtocol());
        }
    }

    private static void readTag(NTClientPacket packet, NBTTagCompound data) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            ByteBufUtils.writeTag(buffer, data);
            packet.fromBytes(buffer);
        } finally {
            buffer.release();
        }
    }
}
