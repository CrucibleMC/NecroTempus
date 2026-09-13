package io.github.cruciblemc.necrotempus.modules.features.glow.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class GlowPacketTest {

    @Test
    void volumeRoundTrips() {
        GlowPacket sent = new GlowPacket(
            GlowPacket.Op.SET_VOLUME,
            12,
            1.0,
            2.0,
            3.0,
            4.0,
            5.0,
            6.0,
            0x00FF00,
            40);
        ByteBuf buffer = Unpooled.buffer();

        sent.toBytes(buffer);
        GlowPacket received = new GlowPacket();
        received.fromBytes(buffer);

        assertEquals(GlowPacket.Op.SET_VOLUME, received.getOp());
        assertEquals(12, received.getEntityId());
        assertEquals(1.0, received.getMinX());
        assertEquals(6.0, received.getMaxZ());
        assertEquals(0x00FF00, received.getRgb());
        assertEquals(40, received.getDurationTicks());
    }
}
