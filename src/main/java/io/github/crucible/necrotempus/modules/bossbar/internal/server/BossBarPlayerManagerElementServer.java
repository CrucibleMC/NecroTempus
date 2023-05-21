package io.github.crucible.necrotempus.modules.bossbar.internal.server;

import io.github.crucible.necrotempus.NecroTempus;
import io.github.crucible.necrotempus.modules.bossbar.api.BossBar;
import io.github.crucible.necrotempus.modules.bossbar.internal.component.BossBarPlayerManagerElement;
import io.github.crucible.necrotempus.modules.bossbar.internal.network.BossBarPacket;
import io.github.crucible.necrotempus.utils.ServerUtils;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class BossBarPlayerManagerElementServer implements BossBarPlayerManagerElement {

    @Override
    public void add(UUID target, BossBar bossBar){
        deliver(Collections.singleton(target), new BossBarPacket(bossBar, BossBarPacket.PacketType.ADD));
    }

    @Override
    public void remove(BossBar bossBar) {
        if(!bossBar.isVisible() || bossBar.hasPlayers())
            deliver(bossBar.getPlayers(), new BossBarPacket(bossBar, BossBarPacket.PacketType.REMOVE));
    }

    @Override
    public void remove(UUID target, BossBar bossBar) {
        deliver(Collections.singleton(target), new BossBarPacket(bossBar, BossBarPacket.PacketType.REMOVE));
    }

    public void deliver(Set<UUID> players, BossBarPacket packet){
        for(UUID uuid : players){
            EntityPlayerMP entityPlayerMP = ServerUtils.getPlayer(uuid);

            if(entityPlayerMP != null)
                NecroTempus.DISPATCHER.sendTo(packet, entityPlayerMP);
        }
    }

}
