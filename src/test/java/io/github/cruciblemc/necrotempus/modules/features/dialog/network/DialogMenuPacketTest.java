package io.github.cruciblemc.necrotempus.modules.features.dialog.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.DialogMenuCatalog;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class DialogMenuPacketTest {

    @Test
    void statePacketRoundTripsRevisionAndAvailableCounts() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            new DialogMenuStatePacket(7, 2, 1, 3).toBytes(buffer);
            DialogMenuStatePacket decoded = new DialogMenuStatePacket();
            decoded.fromBytes(buffer);

            assertTrue(decoded.isValid());
            assertEquals(7, decoded.getRevision());
            assertEquals(2, decoded.getPauseCount());
            assertEquals(1, decoded.getQuickActionCount());
            assertEquals(3, decoded.getServerLinkCount());
        } finally {
            buffer.release();
        }
    }

    @Test
    void statePacketRoundTripsTheResolvedPauseButtonLabel() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            new DialogMenuStatePacket(7, 1, 0, 0, new ChatComponentText("External title")).toBytes(buffer);
            DialogMenuStatePacket decoded = new DialogMenuStatePacket();
            decoded.fromBytes(buffer);

            assertTrue(decoded.isValid());
            assertEquals(
                "External title",
                decoded.getPauseLabel()
                    .getUnformattedText());
        } finally {
            buffer.release();
        }
    }

    @Test
    void statePacketRejectsOversizedPauseLabelsBeforeAllocatingThem() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeInt(1);
            buffer.writeByte(1);
            buffer.writeByte(0);
            buffer.writeByte(0);
            buffer.writeByte(1);
            buffer.writeShort(32768);
            DialogMenuStatePacket decoded = new DialogMenuStatePacket();
            decoded.fromBytes(buffer);
            assertFalse(decoded.isValid());
        } finally {
            buffer.release();
        }
    }

    @Test
    void statePacketRejectsCountsBeyondCatalogLimit() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new DialogMenuStatePacket(1, DialogMenuCatalog.MAX_MENU_ENTRIES + 1, 0, 0));
    }

    @Test
    void statePacketRejectsTooManyServerLinks() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new DialogMenuStatePacket(1, 0, 0, DialogMenuCatalog.MAX_SERVER_LINKS + 1));
    }

    @Test
    void requestPacketRoundTripsMenuKindAndRevision() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            new DialogMenuRequestPacket(DialogMenuCatalog.Kind.QUICK_ACTION, 9).toBytes(buffer);
            DialogMenuRequestPacket decoded = new DialogMenuRequestPacket();
            decoded.fromBytes(buffer);
            assertTrue(decoded.isValid());
            assertEquals(DialogMenuCatalog.Kind.QUICK_ACTION, decoded.getKind());
            assertEquals(9, decoded.getRevision());
        } finally {
            buffer.release();
        }
    }

    @Test
    void requestPacketRoundTripsServerLinksKind() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            new DialogMenuRequestPacket(DialogMenuCatalog.Kind.SERVER_LINKS, 10).toBytes(buffer);
            DialogMenuRequestPacket decoded = new DialogMenuRequestPacket();
            decoded.fromBytes(buffer);
            assertTrue(decoded.isValid());
            assertEquals(DialogMenuCatalog.Kind.SERVER_LINKS, decoded.getKind());
            assertEquals(10, decoded.getRevision());
        } finally {
            buffer.release();
        }
    }
}
