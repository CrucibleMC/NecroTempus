package io.github.cruciblemc.necrotempus.modules.features.dialog.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.github.cruciblemc.necrotempus.api.dialog.DialogMenuCatalog;
import io.netty.buffer.ByteBuf;

public final class DialogMenuRequestPacket implements IMessage {

    private DialogMenuCatalog.Kind kind;
    private int revision;
    private boolean valid;

    public DialogMenuRequestPacket() {}

    public DialogMenuRequestPacket(DialogMenuCatalog.Kind kind, int revision) {
        if (kind == null || revision < 1) throw new IllegalArgumentException("Invalid dialog menu request");
        this.kind = kind;
        this.revision = revision;
        valid = true;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        valid = false;
        if (buffer.readableBytes() != 5) return;
        int kindId = buffer.readUnsignedByte();
        revision = buffer.readInt();
        if (kindId >= DialogMenuCatalog.Kind.values().length || revision < 1) return;
        kind = DialogMenuCatalog.Kind.values()[kindId];
        valid = true;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (!valid) throw new IllegalStateException("Invalid dialog menu request");
        buffer.writeByte(kind.ordinal());
        buffer.writeInt(revision);
    }

    public DialogMenuCatalog.Kind getKind() {
        return kind;
    }

    public int getRevision() {
        return revision;
    }

    public boolean isValid() {
        return valid;
    }
}
