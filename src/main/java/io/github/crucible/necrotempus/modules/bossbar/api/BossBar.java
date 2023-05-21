package io.github.crucible.necrotempus.modules.bossbar.api;

import io.github.crucible.necrotempus.modules.bossbar.internal.component.BossBarComponent;
import io.github.crucible.necrotempus.modules.bossbar.internal.component.BossBarPlayerManagerElement;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static io.github.crucible.necrotempus.modules.bossbar.internal.component.BossBarPlayerManagerElement.commonInstance;

public class BossBar extends BossBarComponent{

    public static BossBarPlayerManagerElement BOSS_BAR_PLAYER_MANAGER_ELEMENT = commonInstance();

    private BossBar() {
        super();
    }

    private BossBar(UUID uuid) {
        super(uuid);
    }

    private BossBar(NBTTagCompound tagCompound){
        super(tagCompound);
    }

    private BossBar(UUID uuid, ChatComponentText text, BossBarColor color, BossBarType type, Float percent, boolean isVisible){
        super(uuid, text, color, type, percent, isVisible);
    }

    public static BossBar createBossBar(){
        return new BossBar();
    }

    public static BossBar createBossBar(UUID uuid){
        return new BossBar(uuid);
    }

    public static BossBar createBossBar(NBTTagCompound nbtTagCompound){
        return new BossBar(nbtTagCompound);
    }

    public static BossBar createBossBar(UUID uuid, ChatComponentText text, BossBarColor color, BossBarType type, Float percent, boolean isVisible){
        return new BossBar(uuid, text, color, type, percent, isVisible);
    }

    private final Set<UUID> players = new HashSet<>();

    public Set<UUID> getPlayers() {
        return players;
    }

    public boolean hasPlayer(UUID player){
        return players.contains(player);
    }

    public boolean hasPlayers(){
        return !players.isEmpty();
    }

    public void removePlayer(UUID player){
        players.remove(player);
        BOSS_BAR_PLAYER_MANAGER_ELEMENT.remove(player, this);
    }

    public void addPlayer(UUID player){
        players.add(player);
        BOSS_BAR_PLAYER_MANAGER_ELEMENT.add(player,this);
    }

    public void removeAllPlayers(){
        players.clear();
        BOSS_BAR_PLAYER_MANAGER_ELEMENT.remove(this);
    }

}
