package io.github.cruciblemc.necrotempus.modules.features.dialog.network;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import net.minecraft.util.IChatComponent;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.github.cruciblemc.necrotempus.api.dialog.DialogMenuCatalog;
import io.netty.buffer.ByteBuf;

public final class DialogMenuStatePacket implements IMessage {

    private static final int MAX_LABEL_BYTES = 32767;
    private int revision;
    private int pauseCount;
    private int quickActionCount;
    private int serverLinkCount;
    private IChatComponent pauseLabel;
    private boolean valid;

    public DialogMenuStatePacket() {}

    public DialogMenuStatePacket(int revision, int pauseCount, int quickActionCount, int serverLinkCount) {
        this(revision, pauseCount, quickActionCount, serverLinkCount, null);
    }

    public DialogMenuStatePacket(int revision, int pauseCount, int quickActionCount, int serverLinkCount,
        IChatComponent pauseLabel) {
        if (revision < 0 || pauseCount < 0
            || pauseCount > DialogMenuCatalog.MAX_MENU_ENTRIES
            || quickActionCount < 0
            || quickActionCount > DialogMenuCatalog.MAX_MENU_ENTRIES
            || serverLinkCount < 0
            || serverLinkCount > DialogMenuCatalog.MAX_SERVER_LINKS) {
            throw new IllegalArgumentException("Invalid dialog menu state");
        }
        this.revision = revision;
        this.pauseCount = pauseCount;
        this.quickActionCount = quickActionCount;
        this.serverLinkCount = serverLinkCount;
        this.pauseLabel = pauseLabel;
        if (pauseLabel != null && componentBytes(pauseLabel).length > MAX_LABEL_BYTES) {
            throw new IllegalArgumentException("Dialog pause label is too large");
        }
        valid = true;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        valid = false;
        pauseLabel = null;
        if (buffer.readableBytes() < 8) return;
        revision = buffer.readInt();
        pauseCount = buffer.readUnsignedByte();
        quickActionCount = buffer.readUnsignedByte();
        serverLinkCount = buffer.readUnsignedByte();
        int hasLabel = buffer.readUnsignedByte();
        if (hasLabel > 1) return;
        if (hasLabel == 1) {
            if (buffer.readableBytes() < 2) return;
            int length = buffer.readUnsignedShort();
            if (length == 0 || length > MAX_LABEL_BYTES || length > buffer.readableBytes()) return;
            byte[] bytes = new byte[length];
            buffer.readBytes(bytes);
            try {
                String json = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
                pauseLabel = IChatComponent.Serializer.func_150699_a(json);
            } catch (CharacterCodingException | RuntimeException exception) {
                return;
            }
            if (pauseLabel == null) return;
        }
        if (buffer.isReadable()) return;
        valid = revision >= 0 && pauseCount <= DialogMenuCatalog.MAX_MENU_ENTRIES
            && quickActionCount <= DialogMenuCatalog.MAX_MENU_ENTRIES
            && serverLinkCount <= DialogMenuCatalog.MAX_SERVER_LINKS;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (!valid) throw new IllegalStateException("Invalid dialog menu state");
        buffer.writeInt(revision);
        buffer.writeByte(pauseCount);
        buffer.writeByte(quickActionCount);
        buffer.writeByte(serverLinkCount);
        if (pauseLabel == null) {
            buffer.writeByte(0);
        } else {
            byte[] bytes = componentBytes(pauseLabel);
            buffer.writeByte(1);
            buffer.writeShort(bytes.length);
            buffer.writeBytes(bytes);
        }
    }

    public int getRevision() {
        return revision;
    }

    public int getPauseCount() {
        return pauseCount;
    }

    public int getQuickActionCount() {
        return quickActionCount;
    }

    public int getServerLinkCount() {
        return serverLinkCount;
    }

    public IChatComponent getPauseLabel() {
        return pauseLabel;
    }

    public boolean isValid() {
        return valid;
    }

    private static byte[] componentBytes(IChatComponent component) {
        return IChatComponent.Serializer.func_150696_a(component)
            .getBytes(StandardCharsets.UTF_8);
    }
}
