package io.github.crucible.necrotempus.modules.bossbar.internal.server;

import io.github.crucible.necrotempus.CrucibleNecroTempus;
import io.github.crucible.necrotempus.modules.bossbar.api.BossBarApi;
import io.github.crucible.necrotempus.modules.bossbar.internal.api.BossBar;
import io.github.crucible.necrotempus.modules.bossbar.internal.api.BossBarColor;
import io.github.crucible.necrotempus.modules.bossbar.internal.network.BossBarPacket;
import io.github.crucible.necrotempus.modules.bossbar.internal.api.BossBarType;
import io.github.crucible.necrotempus.utils.ServerUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BossBarApiImpl implements BossBarApi {

    public BossBar createBossBar(){
        return new BossBar();
    }

    public BossBar createBossBar(UUID uuid){
        return new BossBar(uuid);
    }

    public BossBar createBossBar(NBTTagCompound nbtTagCompound){
        return new BossBar(nbtTagCompound);
    }

    public BossBar createBossBar(UUID uuid, ChatComponentText text, BossBarColor color, BossBarType type, Float percent, boolean isVisible){
        return new BossBar(uuid, text, color, type, percent, isVisible);
    }

    public void add(BossBar bossBar){
        deliver(bossBar.getPlayers(), new BossBarPacket(bossBar, BossBarPacket.PacketType.ADD));
    }

    public void update(BossBar bossBar){
        deliver(bossBar.getPlayers(), new BossBarPacket(bossBar, BossBarPacket.PacketType.UPDATE));
    }

    public void remove(BossBar bossBar){
        deliver(bossBar.getPlayers(), new BossBarPacket(bossBar, BossBarPacket.PacketType.REMOVE));
    }

    private void deliver(Set<UUID> players, BossBarPacket packet){
        for(UUID uuid : players){
            EntityPlayerMP entityPlayerMP = ServerUtils.getPlayer(uuid);

            if(entityPlayerMP != null)
                CrucibleNecroTempus.DISPATCHER.sendTo(packet, entityPlayerMP);
        }
    }

}
