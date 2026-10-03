package io.github.cruciblemc.necrotempus.modules.features.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class ChatHeadPacketTest {

    @Test
    void roundTripsIdentityAndInteractiveComponent() {
        UUID sender = UUID.randomUUID();
        IChatComponent message = new ChatComponentText("<Nick> hi");
        message.getChatStyle()
            .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg Nick "))
            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("profile")));

        ChatHeadPacket packet = new ChatHeadPacket(sender, "[VIP] Nick", message, true);
        ByteBuf buffer = Unpooled.buffer();
        packet.toBytes(buffer);

        ChatHeadPacket decoded = new ChatHeadPacket();
        decoded.fromBytes(buffer);

        assertEquals(sender, decoded.getSenderUuid());
        assertEquals("[VIP] Nick", decoded.getTargetName());
        assertEquals(
            "<Nick> hi",
            decoded.getComponent()
                .getUnformattedText());
        assertEquals(
            ClickEvent.Action.SUGGEST_COMMAND,
            decoded.getComponent()
                .getChatStyle()
                .getChatClickEvent()
                .getAction());
        assertNotNull(
            decoded.getComponent()
                .getChatStyle()
                .getChatHoverEvent());
        assertEquals(true, decoded.isChat());
    }

    @Test
    void ignoresIncompletePayloadAndClearsPreviousDecodedValue() {
        ChatHeadPacket decoded = new ChatHeadPacket();
        ByteBuf valid = Unpooled.buffer();
        new ChatHeadPacket(UUID.randomUUID(), "Nick", new ChatComponentText("hello"), true).toBytes(valid);
        decoded.fromBytes(valid);
        assertEquals(true, decoded.isValid());

        decoded.fromBytes(
            Unpooled.buffer()
                .writeLong(1L));

        assertFalse(decoded.isValid());
        assertEquals(null, decoded.getComponent());
        assertEquals(null, decoded.getSenderUuid());
    }

    @Test
    void rejectsInvalidFlagAndTrailingPayloadBytes() {
        ByteBuf invalidFlag = validPayload();
        invalidFlag.setByte(16, 2);

        ChatHeadPacket decoded = new ChatHeadPacket();
        decoded.fromBytes(invalidFlag);
        assertFalse(decoded.isValid());

        ByteBuf trailingData = validPayload();
        trailingData.writeByte(0);
        decoded.fromBytes(trailingData);
        assertFalse(decoded.isValid());
    }

    private static ByteBuf validPayload() {
        ByteBuf buffer = Unpooled.buffer();
        new ChatHeadPacket(UUID.randomUUID(), "Nick", new ChatComponentText("hello"), true).toBytes(buffer);
        return buffer;
    }
}
