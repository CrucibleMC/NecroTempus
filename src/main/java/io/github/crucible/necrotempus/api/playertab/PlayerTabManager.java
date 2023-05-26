package io.github.crucible.necrotempus.api.playertab;

import io.github.crucible.necrotempus.NecroTempus;
import io.github.crucible.necrotempus.modules.features.playertab.network.PlayerTabPacket;
import io.github.crucible.necrotempus.utils.ServerUtils;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

public class PlayerTabManager {

    public void set(HashSet<UUID> players, PlayerTab tab){
        deliver(players, new PlayerTabPacket(tab, PlayerTabPacket.PacketType.SET));
    }

    public void update(HashSet<UUID> players, PlayerTab tab){
        deliver(players, new PlayerTabPacket(tab, PlayerTabPacket.PacketType.UPDATE));
    }

    public void remove(HashSet<UUID> players){
        deliver(players, new PlayerTabPacket(
                new PlayerTab(new ArrayList<>(), false, null, null),
                PlayerTabPacket.PacketType.REMOVE)
        );
    }

    private void deliver(HashSet<UUID> players, PlayerTabPacket packet){
        for(UUID uuid : players){
            EntityPlayerMP entityPlayerMP = ServerUtils.getPlayer(uuid);
            if(entityPlayerMP != null)
                NecroTempus.DISPATCHER.sendTo(packet, entityPlayerMP);
        }
    }

    public static PlayerTabManager commonInstance(){
        return new PlayerTabManager();
    }

}
