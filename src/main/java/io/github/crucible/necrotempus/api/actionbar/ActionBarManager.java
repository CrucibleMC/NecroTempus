package io.github.crucible.necrotempus.api.actionbar;

import io.github.crucible.necrotempus.NecroTempus;
import io.github.crucible.necrotempus.api.playertab.PlayerTabManager;
import io.github.crucible.necrotempus.modules.features.actionbar.network.ActionBarPacket;
import io.github.crucible.necrotempus.utils.ServerUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import java.util.HashSet;
import java.util.UUID;

public class ActionBarManager {

    public void set(HashSet<UUID> players, ActionBar tab){
        deliver(players, new ActionBarPacket(tab, ActionBarPacket.PacketType.SET));
    }

    public void remove(HashSet<UUID> players){
        deliver(players, new ActionBarPacket(
                new ActionBar(0, new ChatComponentText("")),
                ActionBarPacket.PacketType.REMOVE)
        );
    }

    private void deliver(HashSet<UUID> players, ActionBarPacket packet){
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
