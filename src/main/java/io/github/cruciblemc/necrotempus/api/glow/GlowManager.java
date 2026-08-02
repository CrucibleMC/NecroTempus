package io.github.cruciblemc.necrotempus.api.glow;

import net.minecraft.entity.player.EntityPlayerMP;

import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.modules.features.glow.network.GlowPacket;

/** Server-side entry point: tell a viewer that an entity glows. Mirrors TitleManager/ActionBarManager. */
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

    public static void clear(EntityPlayerMP viewer) {
        if (viewer != null) {
            NecroTempus.DISPATCHER.sendTo(new GlowPacket(GlowPacket.Op.CLEAR, 0, -1, 0), viewer);
        }
    }
}
