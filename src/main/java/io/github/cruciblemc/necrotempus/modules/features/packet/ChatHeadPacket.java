package io.github.cruciblemc.necrotempus.modules.features.packet;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.util.IChatComponent;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class ChatHeadPacket implements IMessage {

    private static final int MAX_COMPONENT_BYTES = 32767;
    private static final int MAX_SENDER_NAME_BYTES = 1024;

    private UUID senderUuid;
    private String targetName;
    private String displayName;
    private IChatComponent component;
    private boolean chat;
    private boolean valid;

    public ChatHeadPacket() {}

    public ChatHeadPacket(UUID senderUuid, String targetName, String displayName, IChatComponent component,
        boolean chat) {
        if (senderUuid == null || component == null) throw new IllegalArgumentException("Missing chat head data");
        this.senderUuid = senderUuid;
        this.targetName = targetName == null ? "" : targetName;
        this.displayName = displayName == null ? "" : displayName;
        this.component = component;
        this.chat = chat;
        this.valid = true;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        valid = false;
        senderUuid = null;
        targetName = null;
        displayName = null;
        component = null;
        chat = false;
        if (buffer.readableBytes() < 29) return;

        UUID uuid = new UUID(buffer.readLong(), buffer.readLong());
        byte chatValue = buffer.readByte();
        int nameLength = buffer.readInt();
        if (nameLength < 0 || nameLength > MAX_SENDER_NAME_BYTES || nameLength > buffer.readableBytes() - 8) return;
        byte[] nameBytes = new byte[nameLength];
        buffer.readBytes(nameBytes);
        int displayNameLength = buffer.readInt();
        if (displayNameLength < 0 || displayNameLength > MAX_SENDER_NAME_BYTES
            || displayNameLength > buffer.readableBytes() - 4) return;
        byte[] displayNameBytes = new byte[displayNameLength];
        buffer.readBytes(displayNameBytes);
        int length = buffer.readInt();

        if ((chatValue != 0 && chatValue != 1) || length < 0
            || length > MAX_COMPONENT_BYTES
            || length != buffer.readableBytes()) return;

        byte[] json = new byte[length];
        buffer.readBytes(json);

        try {
            IChatComponent decoded = IChatComponent.Serializer.func_150699_a(new String(json, StandardCharsets.UTF_8));
            if (decoded == null) return;
            senderUuid = uuid;
            targetName = new String(nameBytes, StandardCharsets.UTF_8);
            displayName = new String(displayNameBytes, StandardCharsets.UTF_8);
            component = decoded;
            chat = chatValue == 1;
            valid = true;
        } catch (RuntimeException ignored) {
            senderUuid = null;
            targetName = null;
            displayName = null;
            component = null;
            chat = false;
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (senderUuid == null || component == null) throw new IllegalStateException("Missing chat head data");

        byte[] json = IChatComponent.Serializer.func_150696_a(component)
            .getBytes(StandardCharsets.UTF_8);
        byte[] name = (targetName == null ? "" : targetName).getBytes(StandardCharsets.UTF_8);
        byte[] display = (displayName == null ? "" : displayName).getBytes(StandardCharsets.UTF_8);
        if (display.length > MAX_SENDER_NAME_BYTES) display = new byte[0];
        if (json.length > MAX_COMPONENT_BYTES || name.length > MAX_SENDER_NAME_BYTES) {
            throw new IllegalArgumentException("Chat head payload is too large");
        }

        buffer.writeLong(senderUuid.getMostSignificantBits());
        buffer.writeLong(senderUuid.getLeastSignificantBits());
        buffer.writeByte(chat ? 1 : 0);
        buffer.writeInt(name.length);
        buffer.writeBytes(name);
        buffer.writeInt(display.length);
        buffer.writeBytes(display);
        buffer.writeInt(json.length);
        buffer.writeBytes(json);
    }

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public IChatComponent getComponent() {
        return component;
    }

    public boolean isChat() {
        return chat;
    }

    public boolean isValid() {
        return valid;
    }
}
