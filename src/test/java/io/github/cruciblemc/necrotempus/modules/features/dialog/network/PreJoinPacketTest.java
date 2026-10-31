package io.github.cruciblemc.necrotempus.modules.features.dialog.network;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.Test;

import cpw.mods.fml.relauncher.Side;
import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class PreJoinPacketTest {

    @Test
    void roundTripsEveryOperationAndPreservesDynamicFormMarker() {
        UUID id = UUID.randomUUID();
        PreJoinPacket hello = roundTrip(PreJoinPacket.hello());
        PreJoinPacket capability = roundTrip(PreJoinPacket.capability(2));
        PreJoinPacket show = roundTrip(PreJoinPacket.show(id, 2, dynamicDialog()));
        PreJoinPacket clear = roundTrip(PreJoinPacket.clear(id, 2));
        PreJoinPacket opened = roundTrip(PreJoinPacket.opened(id, 2));
        PreJoinPacket submit = roundTrip(PreJoinPacket.submit(id, 2, 0, Collections.singletonMap("name", "Alex")));
        PreJoinPacket disconnect = roundTrip(PreJoinPacket.disconnect());

        assertEquals(PreJoinPacket.Operation.HELLO, hello.getOperation());
        assertEquals(2, capability.getCapabilityVersion());
        assertTrue(
            show.getDialog()
                .getActions()
                .get(0)
                .getAction()
                .isDynamic());
        assertEquals(id, clear.getSessionId());
        assertEquals(id, opened.getSessionId());
        assertEquals(
            "Alex",
            submit.getValues()
                .get("name"));
        assertEquals(PreJoinPacket.Operation.DISCONNECT, disconnect.getOperation());
    }

    @Test
    void enforcesCompleteOperationDirectionMatrixAndNullFailsClosed() {
        PreJoinPacket[] serverToClient = { PreJoinPacket.hello(),
            PreJoinPacket.show(UUID.randomUUID(), 1, dynamicDialog()), PreJoinPacket.clear(UUID.randomUUID(), 1), };
        PreJoinPacket[] clientToServer = { PreJoinPacket.capability(1), PreJoinPacket.opened(UUID.randomUUID(), 1),
            PreJoinPacket.submit(UUID.randomUUID(), 1, 0, Collections.emptyMap()), PreJoinPacket.disconnect(), };
        for (PreJoinPacket packet : serverToClient) {
            assertTrue(
                packet.isValidFor(Side.CLIENT),
                packet.getOperation()
                    .name());
            assertFalse(
                packet.isValidFor(Side.SERVER),
                packet.getOperation()
                    .name());
            assertFalse(
                packet.isValidFor(null),
                packet.getOperation()
                    .name());
        }
        for (PreJoinPacket packet : clientToServer) {
            assertTrue(
                packet.isValidFor(Side.SERVER),
                packet.getOperation()
                    .name());
            assertFalse(
                packet.isValidFor(Side.CLIENT),
                packet.getOperation()
                    .name());
            assertFalse(
                packet.isValidFor(null),
                packet.getOperation()
                    .name());
        }
    }

    @Test
    void rejectsUnsupportedEnvelopeVersionAndResetsAllFieldsAfterFailedReuse() {
        ByteBuf validCapability = Unpooled.buffer();
        PreJoinPacket.capability(2)
            .toBytes(validCapability);
        PreJoinPacket reused = new PreJoinPacket();
        reused.fromBytes(validCapability);
        assertTrue(reused.isValid());
        assertEquals(2, reused.getCapabilityVersion());

        ByteBuf unsupportedVersion = Unpooled.buffer();
        unsupportedVersion.writeByte(2);
        unsupportedVersion.writeByte(PreJoinPacket.Operation.SUBMIT.ordinal());
        unsupportedVersion.writeLong(1L);
        unsupportedVersion.writeLong(2L);
        unsupportedVersion.writeInt(4);
        unsupportedVersion.writeInt(0);
        reused.fromBytes(unsupportedVersion);
        assertFalse(reused.isValid());
        assertFalse(reused.isValidFor(Side.SERVER));
        assertEquals(null, reused.getOperation());
        assertEquals(0, reused.getCapabilityVersion());
        assertEquals(null, reused.getSessionId());
        assertEquals(0, reused.getRevision());
        assertEquals(null, reused.getDialog());
        assertEquals(0, reused.getActionIndex());
        assertTrue(
            reused.getValues()
                .isEmpty());

        ByteBuf malformedCapability = Unpooled.buffer();
        malformedCapability.writeByte(1);
        malformedCapability.writeByte(PreJoinPacket.Operation.CAPABILITY.ordinal());
        reused.fromBytes(malformedCapability);
        assertFalse(reused.isValid());
        assertEquals(0, reused.getCapabilityVersion());
        assertEquals(null, reused.getOperation());
    }

    @Test
    void rejectsTruncationTrailingBytesAndOversizedFrames() {
        ByteBuf truncated = Unpooled.buffer();
        truncated.writeByte(1);
        truncated.writeByte(PreJoinPacket.Operation.SHOW.ordinal());
        PreJoinPacket shortPacket = new PreJoinPacket();
        shortPacket.fromBytes(truncated);
        assertFalse(shortPacket.isValid());

        ByteBuf trailing = Unpooled.buffer();
        PreJoinPacket.hello()
            .toBytes(trailing);
        trailing.writeByte(0);
        PreJoinPacket extraPacket = new PreJoinPacket();
        extraPacket.fromBytes(trailing);
        assertFalse(extraPacket.isValid());

        ByteBuf oversized = Unpooled.buffer(65537);
        oversized.writeZero(65537);
        PreJoinPacket tooLarge = new PreJoinPacket();
        tooLarge.fromBytes(oversized);
        assertFalse(tooLarge.isValid());

        ByteBuf oversizedClientFrame = Unpooled.buffer(32767);
        oversizedClientFrame.writeByte(1);
        oversizedClientFrame.writeByte(PreJoinPacket.Operation.CAPABILITY.ordinal());
        oversizedClientFrame.writeZero(32765);
        PreJoinPacket clientTooLarge = new PreJoinPacket();
        clientTooLarge.fromBytes(oversizedClientFrame);
        assertFalse(clientTooLarge.isValid());
    }

    @Test
    void appliesDirectionSpecificFrameCeilingsToFrameValidationAndServerShowEncoding() {
        assertDoesNotThrow(() -> PreJoinPacket.validateFrameSize(PreJoinPacket.Operation.SUBMIT, 32765));
        assertThrows(
            IllegalArgumentException.class,
            () -> PreJoinPacket.validateFrameSize(PreJoinPacket.Operation.SUBMIT, 32766));
        assertDoesNotThrow(() -> PreJoinPacket.validateFrameSize(PreJoinPacket.Operation.SHOW, 65536));
        assertThrows(
            IllegalArgumentException.class,
            () -> PreJoinPacket.validateFrameSize(PreJoinPacket.Operation.SHOW, 65537));

        String text = repeat('x', 18000);
        Dialog large = Dialog.notice(new ChatComponentText("large"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .body(new Dialog.TextBody(new ChatComponentText(text)))
            .body(new Dialog.TextBody(new ChatComponentText(text)))
            .action(new Dialog.ActionButton(new ChatComponentText("Continue"), Dialog.Action.none()))
            .build();
        ByteBuf encoded = Unpooled.buffer();
        PreJoinPacket.show(UUID.randomUUID(), 1, large)
            .toBytes(encoded);
        assertTrue(encoded.readableBytes() > 32766);
        assertTrue(encoded.readableBytes() <= 65536);
        PreJoinPacket decoded = new PreJoinPacket();
        decoded.fromBytes(encoded);
        assertTrue(decoded.isValid());
    }

    @Test
    void rejectsResponseValuesAboveDialogValueLimit() {
        StringBuilder oversized = new StringBuilder(17000);
        for (int i = 0; i < 17000; i++) oversized.append('x');
        ByteBuf buffer = Unpooled.buffer();
        assertThrows(
            IllegalArgumentException.class,
            () -> PreJoinPacket.submit(UUID.randomUUID(), 1, 0, Collections.singletonMap("name", oversized.toString()))
                .toBytes(buffer));
    }

    private static Dialog dynamicDialog() {
        return Dialog.notice(new ChatComponentText("Provide a name"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "", 40, 1))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Continue"),
                    Dialog.Action.dynamicCustom("test:submit", new NBTTagCompound())))
            .build();
    }

    private static String repeat(char value, int count) {
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, value);
        return new String(chars);
    }

    private static PreJoinPacket roundTrip(PreJoinPacket packet) {
        ByteBuf buffer = Unpooled.buffer();
        packet.toBytes(buffer);
        PreJoinPacket decoded = new PreJoinPacket();
        decoded.fromBytes(buffer);
        assertTrue(decoded.isValid());
        return decoded;
    }
}
