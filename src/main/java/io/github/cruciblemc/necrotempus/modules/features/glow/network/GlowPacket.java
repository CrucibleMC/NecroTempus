package io.github.cruciblemc.necrotempus.modules.features.glow.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** Server -> client: mark/unmark an entity as glowing. */
public class GlowPacket implements IMessage {

    public enum Op {
        SET,
        REMOVE,
        CLEAR
    }

    private Op op = Op.CLEAR;
    private int entityId;
    private int rgb = -1;
    private int durationTicks;

    public GlowPacket() {}

    public GlowPacket(Op op, int entityId, int rgb, int durationTicks) {
        this.op = op;
        this.entityId = entityId;
        this.rgb = rgb;
        this.durationTicks = durationTicks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.op = Op.values()[buf.readByte()];
        this.entityId = buf.readInt();
        this.rgb = buf.readInt();
        this.durationTicks = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(op.ordinal());
        buf.writeInt(entityId);
        buf.writeInt(rgb);
        buf.writeInt(durationTicks);
    }

    public Op getOp() {
        return op;
    }

    public int getEntityId() {
        return entityId;
    }

    public int getRgb() {
        return rgb;
    }

    public int getDurationTicks() {
        return durationTicks;
    }
}
