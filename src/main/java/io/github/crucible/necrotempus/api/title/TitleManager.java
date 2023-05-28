package io.github.crucible.necrotempus.api.title;

import io.github.crucible.necrotempus.NecroTempus;
import io.github.crucible.necrotempus.api.playertab.PlayerTabManager;
import io.github.crucible.necrotempus.modules.features.title.network.TitlePacket;
import io.github.crucible.necrotempus.utils.ServerUtils;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.HashSet;
import java.util.UUID;

public class TitleManager {


    public void set(HashSet<UUID> players, TitleComponent tab){
        deliver(players, new TitlePacket(tab, TitlePacket.PacketType.SET));
    }

    public void remove(HashSet<UUID> players){
        deliver(players, new TitlePacket(
                new TitleComponent(),
                TitlePacket.PacketType.REMOVE)
        );
    }

    private void deliver(HashSet<UUID> players, TitlePacket packet){
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
