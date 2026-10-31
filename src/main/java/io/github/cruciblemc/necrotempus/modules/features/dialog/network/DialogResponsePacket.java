package io.github.cruciblemc.necrotempus.modules.features.dialog.network;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.github.cruciblemc.necrotempus.modules.features.dialog.DialogFormValues;
import io.netty.buffer.ByteBuf;

public final class DialogResponsePacket implements IMessage {

    public enum Operation {
        OPENED,
        ACTION,
        CANCEL,
        CLOSED_BY_OTHER_GUI,
        REJECTED,
        DISMISS
    }

    private Operation operation;
    private UUID sessionId;
    private int revision;
    private int actionIndex;
    private Map<String, Object> values = Collections.emptyMap();
    private boolean valid;

    public DialogResponsePacket() {}

    private DialogResponsePacket(Operation operation, UUID sessionId, int revision, int actionIndex) {
        this(operation, sessionId, revision, actionIndex, Collections.emptyMap());
    }

    private DialogResponsePacket(Operation operation, UUID sessionId, int revision, int actionIndex,
        Map<String, Object> values) {
        if (operation == null || sessionId == null || revision < 1)
            throw new IllegalArgumentException("Invalid dialog response");
        if (operation == Operation.ACTION && actionIndex < 0)
            throw new IllegalArgumentException("Invalid action index");
        this.operation = operation;
        this.sessionId = sessionId;
        this.revision = revision;
        this.actionIndex = actionIndex;
        this.values = values == null ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(values));
        this.valid = true;
    }

    public static DialogResponsePacket opened(UUID sessionId, int revision) {
        return new DialogResponsePacket(Operation.OPENED, sessionId, revision, 0);
    }

    public static DialogResponsePacket action(UUID sessionId, int revision, int actionIndex) {
        return new DialogResponsePacket(Operation.ACTION, sessionId, revision, actionIndex);
    }

    public static DialogResponsePacket action(UUID sessionId, int revision, int actionIndex,
        Map<String, Object> values) {
        return new DialogResponsePacket(Operation.ACTION, sessionId, revision, actionIndex, values);
    }

    public static DialogResponsePacket cancel(UUID sessionId, int revision) {
        return new DialogResponsePacket(Operation.CANCEL, sessionId, revision, 0);
    }

    public static DialogResponsePacket cancel(UUID sessionId, int revision, Map<String, Object> values) {
        return new DialogResponsePacket(Operation.CANCEL, sessionId, revision, 0, values);
    }

    public static DialogResponsePacket closedByOtherGui(UUID sessionId, int revision) {
        return new DialogResponsePacket(Operation.CLOSED_BY_OTHER_GUI, sessionId, revision, 0);
    }

    public static DialogResponsePacket rejected(UUID sessionId, int revision) {
        return new DialogResponsePacket(Operation.REJECTED, sessionId, revision, 0);
    }

    public static DialogResponsePacket dismiss(UUID sessionId, int revision) {
        return new DialogResponsePacket(Operation.DISMISS, sessionId, revision, 0);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        int start = buffer.readerIndex();
        valid = false;
        if (buffer.readableBytes() < 21) return;
        int operationId = buffer.readUnsignedByte();
        if (operationId >= Operation.values().length) return;
        operation = Operation.values()[operationId];
        sessionId = new UUID(buffer.readLong(), buffer.readLong());
        revision = buffer.readInt();
        if (revision < 1) return;
        actionIndex = 0;
        if (operation == Operation.ACTION) {
            if (buffer.readableBytes() < 4) return;
            actionIndex = buffer.readInt();
            if (actionIndex < 0) return;
        }
        try {
            values = operation == Operation.ACTION || operation == Operation.CANCEL ? DialogFormValues.read(buffer)
                : Collections.emptyMap();
            valid = !buffer.isReadable();
        } catch (RuntimeException exception) {
            buffer.readerIndex(start);
            values = Collections.emptyMap();
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (!valid) throw new IllegalStateException("Invalid dialog response");
        buffer.writeByte(operation.ordinal());
        buffer.writeLong(sessionId.getMostSignificantBits());
        buffer.writeLong(sessionId.getLeastSignificantBits());
        buffer.writeInt(revision);
        if (operation == Operation.ACTION) buffer.writeInt(actionIndex);
        if (operation == Operation.ACTION || operation == Operation.CANCEL) DialogFormValues.write(buffer, values);
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

    public int getActionIndex() {
        return actionIndex;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public boolean isValid() {
        return valid;
    }
}
