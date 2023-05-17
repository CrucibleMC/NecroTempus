package io.github.crucible.necrotempus.modules.bossbar.internal.api;

import io.github.crucible.necrotempus.modules.bossbar.internal.component.BossBarComponent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BossBar extends BossBarComponent {

    private final Set<UUID> players = new HashSet<>();

    private long creationTime;

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public BossBar() {
        super();
    }

    public BossBar(UUID uuid) {
        super(uuid);
    }

    public BossBar(NBTTagCompound tagCompound){
        super(tagCompound);
    }

    public BossBar(UUID uuid, ChatComponentText text, BossBarColor color, BossBarType type, Float percent, boolean isVisible){
        super(uuid, text, color, type, percent, isVisible);
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

    public void clear(){}
}
