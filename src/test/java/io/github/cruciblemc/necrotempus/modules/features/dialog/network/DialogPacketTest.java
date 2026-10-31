package io.github.cruciblemc.necrotempus.modules.features.dialog.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class DialogPacketTest {

    @Test
    void showAndClearCarryTheSameSessionRevision() {
        UUID id = UUID.randomUUID();
        Dialog source = Dialog.confirmation(new ChatComponentText("Test"))
            .action(new Dialog.ActionButton(new ChatComponentText("Yes"), Dialog.Action.none()))
            .action(new Dialog.ActionButton(new ChatComponentText("No"), Dialog.Action.none()))
            .build();
        DialogPacket show = roundTrip(DialogPacket.show(id, 4, source));
        assertTrue(show.isValid());
        assertEquals(DialogPacket.Operation.SHOW, show.getOperation());
        assertEquals(id, show.getSessionId());
        assertEquals(4, show.getRevision());
        assertEquals(
            Dialog.Type.CONFIRMATION,
            show.getDialog()
                .getType());

        DialogPacket clear = roundTrip(DialogPacket.clear(id, 4));
        assertTrue(clear.isValid());
        assertEquals(DialogPacket.Operation.CLEAR, clear.getOperation());
        assertEquals(id, clear.getSessionId());
    }

    @Test
    void responseContainsOnlyAuthorizedActionIndexAndRejectsTrailingBytes() {
        UUID id = UUID.randomUUID();
        DialogResponsePacket response = roundTrip(DialogResponsePacket.action(id, 2, 1));
        assertTrue(response.isValid());
        assertEquals(DialogResponsePacket.Operation.ACTION, response.getOperation());
        assertEquals(id, response.getSessionId());
        assertEquals(1, response.getActionIndex());

        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogResponsePacket.cancel(id, 2)
                .toBytes(buffer);
            buffer.writeByte(9);
            DialogResponsePacket decoded = new DialogResponsePacket();
            decoded.fromBytes(buffer);
            assertFalse(decoded.isValid());
        } finally {
            buffer.release();
        }
    }

    @Test
    void responseRoundTripsTypedFormValues() {
        UUID id = UUID.randomUUID();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "Ada");
        values.put("enabled", true);
        values.put("amount", 2.5f);
        DialogResponsePacket response = roundTrip(DialogResponsePacket.action(id, 3, 0, values));
        assertTrue(response.isValid());
        assertEquals(
            "Ada",
            response.getValues()
                .get("name"));
        assertEquals(
            true,
            response.getValues()
                .get("enabled"));
        assertEquals(
            2.5f,
            response.getValues()
                .get("amount"));

        DialogResponsePacket empty = roundTrip(DialogResponsePacket.action(id, 3, 0, Collections.emptyMap()));
        assertTrue(empty.isValid());
        assertTrue(
            empty.getValues()
                .isEmpty());
    }

    @Test
    void waitingDismissalRoundTripsWithoutFormValues() {
        UUID id = UUID.randomUUID();
        DialogResponsePacket response = roundTrip(DialogResponsePacket.dismiss(id, 7));
        assertTrue(response.isValid());
        assertEquals(DialogResponsePacket.Operation.DISMISS, response.getOperation());
        assertEquals(id, response.getSessionId());
        assertEquals(7, response.getRevision());
        assertTrue(
            response.getValues()
                .isEmpty());
    }

    private static <T extends cpw.mods.fml.common.network.simpleimpl.IMessage> T roundTrip(T source) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            source.toBytes(buffer);
            @SuppressWarnings("unchecked")
            T result = (T) (source instanceof DialogPacket ? new DialogPacket() : new DialogResponsePacket());
            result.fromBytes(buffer);
            return result;
        } finally {
            buffer.release();
        }
    }
}
