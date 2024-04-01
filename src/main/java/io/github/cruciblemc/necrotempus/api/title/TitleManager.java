package io.github.cruciblemc.necrotempus.api.title;

import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.api.playertab.PlayerTabManager;
import io.github.cruciblemc.necrotempus.modules.features.title.network.TitlePacket;
import io.github.cruciblemc.necrotempus.utils.ServerUtils;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.HashSet;
import java.util.UUID;

public class TitleManager {


    public void set(HashSet<UUID> players, TitleComponent tab) {
        deliver(players, new TitlePacket(tab, TitlePacket.PacketType.SET));
    }

    public void remove(HashSet<UUID> players) {
        deliver(players, new TitlePacket(
                new TitleComponent(),
                TitlePacket.PacketType.REMOVE)
        );
    }

    private void deliver(HashSet<UUID> players, TitlePacket packet) {
        for (UUID uuid : players) {
            EntityPlayerMP entityPlayerMP = ServerUtils.getPlayer(uuid);
            if (entityPlayerMP != null)
                NecroTempus.DISPATCHER.sendTo(packet, entityPlayerMP);
        }
    }

}
