package io.github.cruciblemc.necrotempus.modules.features.dialog.network;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.relauncher.Side;
import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.modules.features.dialog.DialogCodec;
import io.github.cruciblemc.necrotempus.modules.features.dialog.DialogFormValues;
import io.github.cruciblemc.necrotempus.modules.features.dialog.PreJoinDialogPolicy;
import io.netty.buffer.ByteBuf;

public final class PreJoinPacket implements IMessage {

    public static final int PROTOCOL_VERSION = 1;
    public static final int MAX_FRAME_BYTES = 65536;
    // FML adds one discriminator byte that counts toward Crucible's 32766-byte packet limit.
    static final int MAX_CLIENT_TO_SERVER_FRAME_BYTES = 32765;

    public enum Operation {

        HELLO(true),
        CAPABILITY(false),
        SHOW(true),
        CLEAR(true),
        OPENED(false),
        SUBMIT(false),
        DISCONNECT(false);

        private final boolean fromServer;

        Operation(boolean fromServer) {
            this.fromServer = fromServer;
        }
    }

    private int protocolVersion = PROTOCOL_VERSION;
    private Operation operation;
    private int capabilityVersion;
    private UUID sessionId;
    private int revision;
    private Dialog dialog;
    private int actionIndex;
    private Map<String, Object> values = Collections.emptyMap();
    private boolean valid;

    public PreJoinPacket() {}

    private PreJoinPacket(Operation operation) {
        this.operation = operation;
        valid = true;
    }

    private PreJoinPacket(Operation operation, UUID sessionId, int revision) {
        this(operation);
        if (sessionId == null || revision < 1) throw new IllegalArgumentException("Invalid dialog identity");
        this.sessionId = sessionId;
        this.revision = revision;
    }

    public static PreJoinPacket hello() {
        return new PreJoinPacket(Operation.HELLO);
    }

    public static PreJoinPacket capability(int version) {
        if (version < 1 || version > 255) throw new IllegalArgumentException("Invalid capability version");
        PreJoinPacket packet = new PreJoinPacket(Operation.CAPABILITY);
        packet.capabilityVersion = version;
        return packet;
    }

    public static PreJoinPacket show(UUID sessionId, int revision, Dialog dialog) {
        PreJoinDialogPolicy.validate(dialog);
        DialogCodec.requireSupported(dialog);
        PreJoinPacket packet = new PreJoinPacket(Operation.SHOW, sessionId, revision);
        packet.dialog = dialog;
        return packet;
    }

    public static PreJoinPacket clear(UUID sessionId, int revision) {
        return new PreJoinPacket(Operation.CLEAR, sessionId, revision);
    }

    public static PreJoinPacket opened(UUID sessionId, int revision) {
        return new PreJoinPacket(Operation.OPENED, sessionId, revision);
    }

    public static PreJoinPacket submit(UUID sessionId, int revision, int actionIndex, Map<String, Object> values) {
        if (actionIndex < 0) throw new IllegalArgumentException("Invalid action index");
        PreJoinPacket packet = new PreJoinPacket(Operation.SUBMIT, sessionId, revision);
        packet.actionIndex = actionIndex;
        packet.values = values == null ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(values));
        return packet;
    }

    public static PreJoinPacket disconnect() {
        return new PreJoinPacket(Operation.DISCONNECT);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        int start = buffer.readerIndex();
        resetFields();
        try {
            if (buffer.readableBytes() < 2 || buffer.readableBytes() > MAX_FRAME_BYTES) return;
            protocolVersion = buffer.readUnsignedByte();
            int operationId = buffer.readUnsignedByte();
            if (operationId >= Operation.values().length) return;
            operation = Operation.values()[operationId];
            validateFrameSize(operation, buffer.readerIndex() - start + buffer.readableBytes());
            if (protocolVersion != PROTOCOL_VERSION) return;
            switch (operation) {
                case HELLO:
                case DISCONNECT:
                    break;
                case CAPABILITY:
                    capabilityVersion = buffer.readUnsignedByte();
                    if (capabilityVersion < 1) return;
                    break;
                case SHOW:
                    readDialogIdentity(buffer);
                    dialog = DialogCodec.read(buffer);
                    PreJoinDialogPolicy.validate(dialog);
                    if (buffer.isReadable()) return;
                    break;
                case CLEAR:
                case OPENED:
                    readDialogIdentity(buffer);
                    break;
                case SUBMIT:
                    readDialogIdentity(buffer);
                    actionIndex = buffer.readInt();
                    if (actionIndex < 0) return;
                    values = DialogFormValues.read(buffer);
                    break;
                default:
                    return;
            }
            if (buffer.isReadable()) return;
            valid = true;
        } catch (RuntimeException ignored) {
            // Malformed packet data is rejected without logging payload contents.
        } finally {
            if (!valid) {
                buffer.readerIndex(start);
                resetFields();
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (!valid || operation == null) throw new IllegalStateException("Invalid pre-join packet");
        int start = buffer.writerIndex();
        try {
            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(operation.ordinal());
            switch (operation) {
                case HELLO:
                case DISCONNECT:
                    break;
                case CAPABILITY:
                    buffer.writeByte(capabilityVersion);
                    break;
                case SHOW:
                    writeDialogIdentity(buffer);
                    DialogCodec.write(buffer, dialog);
                    break;
                case CLEAR:
                case OPENED:
                    writeDialogIdentity(buffer);
                    break;
                case SUBMIT:
                    writeDialogIdentity(buffer);
                    buffer.writeInt(actionIndex);
                    DialogFormValues.write(buffer, values);
                    break;
                default:
                    throw new IllegalStateException("Unknown pre-join operation");
            }
            validateFrameSize(operation, buffer.writerIndex() - start);
        } catch (RuntimeException exception) {
            buffer.writerIndex(start);
            throw exception;
        }
    }

    public boolean isValidFor(Side receiver) {
        return valid && receiver != null && operation != null && ((receiver == Side.CLIENT) == operation.fromServer);
    }

    public Operation getOperation() {
        return operation;
    }

    public int getCapabilityVersion() {
        return capabilityVersion;
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

    public int getActionIndex() {
        return actionIndex;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public boolean isValid() {
        return valid;
    }

    private void readDialogIdentity(ByteBuf buffer) {
        sessionId = new UUID(buffer.readLong(), buffer.readLong());
        revision = buffer.readInt();
        if (revision < 1) throw new IllegalArgumentException("Invalid dialog revision");
    }

    private void writeDialogIdentity(ByteBuf buffer) {
        if (sessionId == null || revision < 1) throw new IllegalStateException("Invalid dialog identity");
        buffer.writeLong(sessionId.getMostSignificantBits());
        buffer.writeLong(sessionId.getLeastSignificantBits());
        buffer.writeInt(revision);
    }

    private void resetFields() {
        valid = false;
        protocolVersion = PROTOCOL_VERSION;
        operation = null;
        capabilityVersion = 0;
        sessionId = null;
        revision = 0;
        dialog = null;
        actionIndex = 0;
        values = Collections.emptyMap();
    }

    static void validateFrameSize(Operation operation, int frameBytes) {
        int maxBytes = operation != null && operation.fromServer ? MAX_FRAME_BYTES : MAX_CLIENT_TO_SERVER_FRAME_BYTES;
        if (frameBytes < 2 || frameBytes > maxBytes) throw new IllegalArgumentException("Pre-join frame is too large");
    }
}
