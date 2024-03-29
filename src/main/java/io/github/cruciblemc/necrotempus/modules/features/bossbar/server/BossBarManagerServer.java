package io.github.cruciblemc.necrotempus.modules.features.bossbar.server;

import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBar;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarManager;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.network.BossBarPacket;
import io.github.cruciblemc.necrotempus.utils.ServerUtils;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class BossBarManagerServer extends BossBarManager {

    public void add(UUID target, BossBar bossBar) {
        deliver(Collections.singleton(target), new BossBarPacket(bossBar, BossBarPacket.PacketType.ADD));
    }

    public void remove(BossBar bossBar) {
        if (!bossBar.isVisible() || hasPlayers(bossBar))
            deliver(getPlayers(bossBar), new BossBarPacket(bossBar, BossBarPacket.PacketType.REMOVE));
    }

    public void remove(UUID target, BossBar bossBar) {
        deliver(Collections.singleton(target), new BossBarPacket(bossBar, BossBarPacket.PacketType.REMOVE));
    }

    public void deliver(Set<UUID> players, Object packet) {
        if (packet instanceof BossBarPacket bossBarPacket) {
            for (UUID uuid : players) {
                EntityPlayerMP entityPlayerMP = ServerUtils.getPlayer(uuid);

                if (entityPlayerMP != null)
                    NecroTempus.DISPATCHER.sendTo(bossBarPacket, entityPlayerMP);
            }
        }
    }

}
