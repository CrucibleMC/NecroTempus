package io.github.crucible.necrotempus.modules.playertab.api;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.crucible.necrotempus.NecroTempus;
import io.github.crucible.necrotempus.modules.playertab.internal.component.PlayerTab;
import io.github.crucible.necrotempus.modules.playertab.internal.network.PlayerTabPacket;
import io.github.crucible.necrotempus.utils.ServerUtils;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

@SideOnly(Side.SERVER)
public class PlayerTabAPI {

    public static void set(HashSet<UUID> players, PlayerTab tab){
        deliver(players, new PlayerTabPacket(tab, PlayerTabPacket.PacketType.SET));
    }

    public static void update(HashSet<UUID> players, PlayerTab tab){
        deliver(players, new PlayerTabPacket(tab, PlayerTabPacket.PacketType.UPDATE));
    }

    public static void remove(HashSet<UUID> players){
        deliver(players, new PlayerTabPacket(
                new PlayerTab(new ArrayList<>(), false, null, null),
                PlayerTabPacket.PacketType.REMOVE)
        );
    }

    private static void deliver(HashSet<UUID> players, PlayerTabPacket packet){
        for(UUID uuid : players){
            EntityPlayerMP entityPlayerMP = ServerUtils.getPlayer(uuid);

            if(entityPlayerMP != null)
                NecroTempus.DISPATCHER.sendTo(packet, entityPlayerMP);
        }
    }

}
