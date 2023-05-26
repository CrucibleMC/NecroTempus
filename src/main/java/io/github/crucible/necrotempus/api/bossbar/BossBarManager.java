package io.github.crucible.necrotempus.api.bossbar;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class BossBarManager {

    public static BossBarManager commonInstance(){
        return new BossBarManager() {

            @Override
            public void add(UUID target, BossBar bossBar) {}

            @Override
            public void remove(BossBar bossBar) {}

            @Override
            public void remove(UUID target, BossBar bossBar) {}

        };
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

    public void removePlayer(UUID player, BossBar bossBar){
        players.remove(player);
        remove(player, bossBar);
    }

    public void addPlayer(UUID player, BossBar bossBar){
        players.add(player);
        add(player,bossBar);
    }

    public void removeAllPlayers(BossBar bossBar){
        players.clear();
        remove(bossBar);
    }

    public abstract void add(UUID target, BossBar bossBar);

    public abstract void remove(BossBar bossBar);

    public abstract void remove(UUID target, BossBar bossBar);

    public void deliver(Set<UUID> players, Object packet){

    }

}
