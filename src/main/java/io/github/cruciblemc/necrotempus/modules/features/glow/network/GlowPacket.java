package io.github.cruciblemc.necrotempus.modules.features.glow.network;

import lombok.Getter;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

/** Server -> client updates for glowing entities and volumes. */
@Getter
public class GlowPacket implements IMessage {

    public enum Op {
        SET,
        REMOVE,
        CLEAR,
        SET_VOLUME,
        REMOVE_VOLUME
    }

    private Op op = Op.CLEAR;
    private int entityId;
    private int rgb = -1;
    private int durationTicks;
    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;

    public GlowPacket() {}

    public GlowPacket(Op op, int entityId, int rgb, int durationTicks) {
        this.op = op;
        this.entityId = entityId;
        this.rgb = rgb;
        this.durationTicks = durationTicks;
    }

    public GlowPacket(Op op, int volumeId, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
        int rgb, int durationTicks) {
        this(op, volumeId, rgb, durationTicks);
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        Op[] operations = Op.values();
        int ordinal = buf.readUnsignedByte();
        if (ordinal >= operations.length) throw new DecoderException("Unknown glow operation: " + ordinal);
        this.op = operations[ordinal];
        this.entityId = buf.readInt();
        this.rgb = buf.readInt();
        this.durationTicks = buf.readInt();
        if (op == Op.SET_VOLUME) {
            minX = buf.readDouble();
            minY = buf.readDouble();
            minZ = buf.readDouble();
            maxX = buf.readDouble();
            maxY = buf.readDouble();
            maxZ = buf.readDouble();
            if (!Double.isFinite(minX) || !Double.isFinite(minY)
                || !Double.isFinite(minZ)
                || !Double.isFinite(maxX)
                || !Double.isFinite(maxY)
                || !Double.isFinite(maxZ)) {
                throw new DecoderException("Glow volume bounds must be finite");
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(op.ordinal());
        buf.writeInt(entityId);
        buf.writeInt(rgb);
        buf.writeInt(durationTicks);
        if (op == Op.SET_VOLUME) {
            buf.writeDouble(minX);
            buf.writeDouble(minY);
            buf.writeDouble(minZ);
            buf.writeDouble(maxX);
            buf.writeDouble(maxY);
            buf.writeDouble(maxZ);
        }
    }
}
