package io.github.crucible.timemachine.bossbar.server;

import io.github.crucible.timemachine.bossbar.BossBarColor;
import io.github.crucible.timemachine.bossbar.BossBarComponent;
import io.github.crucible.timemachine.bossbar.BossBarType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;


import java.util.Set;
import java.util.UUID;

public class BossBar extends BossBarComponent {

    private Set<UUID> players;

    public BossBar(UUID uuid) {
        super(uuid);
    }

    public BossBar(NBTTagCompound tagCompound){
        super(tagCompound);
    }

    public BossBar(ChatComponentText text, BossBarColor color, BossBarType type, Float percent, boolean isVisible, UUID uuid){
        super(text, color, type, percent, isVisible, uuid);
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public boolean hasPlayer(UUID player){
        return players.contains(player);
    }

    public boolean removePlayer(UUID player){
        return players.remove(player);
    }

    public boolean addPlayer(UUID player){
        return  players.add(player);
    }

    public void clear(){

    }
}
