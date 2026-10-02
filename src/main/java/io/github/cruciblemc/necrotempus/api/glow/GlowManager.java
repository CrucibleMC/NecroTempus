package io.github.cruciblemc.necrotempus.api.glow;

import net.minecraft.entity.player.EntityPlayerMP;

import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.modules.features.glow.network.GlowPacket;

/** Server-side API for sending entity and volume glow updates to one viewer. */
public class GlowManager {

    public static void glow(EntityPlayerMP viewer, int entityId, int rgb, int durationTicks) {
        if (viewer != null) {
            NecroTempus.DISPATCHER.sendTo(new GlowPacket(GlowPacket.Op.SET, entityId, rgb, durationTicks), viewer);
        }
    }

    public static void unglow(EntityPlayerMP viewer, int entityId) {
        if (viewer != null) {
            NecroTempus.DISPATCHER.sendTo(new GlowPacket(GlowPacket.Op.REMOVE, entityId, -1, 0), viewer);
        }
    }

    public static void glowVolume(EntityPlayerMP viewer, int volumeId, double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ, int rgb, int durationTicks) {
        if (viewer != null) {
            NecroTempus.DISPATCHER.sendTo(
                new GlowPacket(
                    GlowPacket.Op.SET_VOLUME,
                    volumeId,
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ,
                    rgb,
                    durationTicks),
                viewer);
        }
    }

    public static void unglowVolume(EntityPlayerMP viewer, int volumeId) {
        if (viewer != null) {
            NecroTempus.DISPATCHER.sendTo(new GlowPacket(GlowPacket.Op.REMOVE_VOLUME, volumeId, -1, 0), viewer);
        }
    }

    public static void clear(EntityPlayerMP viewer) {
        if (viewer != null) {
            NecroTempus.DISPATCHER.sendTo(new GlowPacket(GlowPacket.Op.CLEAR, 0, -1, 0), viewer);
        }
    }
}
