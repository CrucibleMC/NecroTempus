package io.github.cruciblemc.necrotempus.modules.features.dialog.network;

import java.util.UUID;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.modules.features.dialog.DialogCodec;
import io.netty.buffer.ByteBuf;

public final class DialogPacket implements IMessage {

    private static final int MAX_FRAME_BYTES = 65536;

    public enum Operation {
        SHOW,
        CLEAR
    }

    private Operation operation;
    private UUID sessionId;
    private int revision;
    private Dialog dialog;
    private boolean valid;

    public DialogPacket() {}

    private DialogPacket(Operation operation, UUID sessionId, int revision, Dialog dialog) {
        if (operation == null || sessionId == null || revision < 1)
            throw new IllegalArgumentException("Invalid dialog packet");
        this.operation = operation;
        this.sessionId = sessionId;
        this.revision = revision;
        this.dialog = dialog;
        this.valid = true;
    }

    public static DialogPacket show(UUID sessionId, int revision, Dialog dialog) {
        DialogCodec.requireSupported(dialog);
        return new DialogPacket(Operation.SHOW, sessionId, revision, dialog);
    }

    public static DialogPacket clear(UUID sessionId, int revision) {
        return new DialogPacket(Operation.CLEAR, sessionId, revision, null);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        valid = false;
        dialog = null;
        if (buffer.readableBytes() < 21 || buffer.readableBytes() > MAX_FRAME_BYTES) return;
        int operationId = buffer.readUnsignedByte();
        if (operationId >= Operation.values().length) return;
        operation = Operation.values()[operationId];
        sessionId = new UUID(buffer.readLong(), buffer.readLong());
        revision = buffer.readInt();
        if (revision < 1) return;
        try {
            if (operation == Operation.SHOW) dialog = DialogCodec.read(buffer);
            else if (buffer.isReadable()) return;
            valid = true;
        } catch (RuntimeException ignored) {
            dialog = null;
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (!valid || operation == null || sessionId == null || revision < 1) {
            throw new IllegalStateException("Invalid dialog packet");
        }
        buffer.writeByte(operation.ordinal());
        buffer.writeLong(sessionId.getMostSignificantBits());
        buffer.writeLong(sessionId.getLeastSignificantBits());
        buffer.writeInt(revision);
        if (operation == Operation.SHOW) DialogCodec.write(buffer, dialog);
    }

    public Operation getOperation() {
        return operation;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public int getRevision() {
        return revision;
    }

    public Dialog getDialog() {
        return dialog;
    }

    public boolean isValid() {
        return valid;
    }
}
