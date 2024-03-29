package io.github.cruciblemc.necrotempus.api.bossbar;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;

public abstract class BossBarManager {

    public static BossBarManager commonInstance() {
        return new BossBarManager() {

            @Override
            public void add(UUID target, BossBar bossBar) {
            }

            @Override
            public void remove(BossBar bossBar) {
            }

            @Override
            public void remove(UUID target, BossBar bossBar) {
            }

        };
    }

    private static final LinkedHashMap<UUID, HashSet<UUID>> players = new LinkedHashMap<>();

    public Set<UUID> getPlayers(BossBar bossBar) {
        return getOrCreatePlayers(bossBar);
    }

    public boolean hasPlayer(BossBar bossBar, UUID player) {
        return getPlayers(bossBar).contains(player);
    }

    public boolean hasPlayers(BossBar bossBar) {
        return !getPlayers(bossBar).isEmpty();
    }

    public void removePlayer(UUID player, BossBar bossBar) {
        getPlayers(bossBar).remove(player);
        remove(player, bossBar);
    }

    public void addPlayer(UUID player, BossBar bossBar) {
        getPlayers(bossBar).add(player);
        add(player, bossBar);
    }

    public void removeAllPlayers(BossBar bossBar) {
        getPlayers(bossBar).clear();
        remove(bossBar);
    }

    private Set<UUID> getOrCreatePlayers(BossBar bossBar) {

        if (!players.containsKey(bossBar.getUuid())) {
            players.put(bossBar.getUuid(), new HashSet<>());
        }

        return players.get(bossBar.getUuid());
    }

    public abstract void add(UUID target, BossBar bossBar);

    public abstract void remove(BossBar bossBar);

    public abstract void remove(UUID target, BossBar bossBar);

    public void deliver(Set<UUID> players, Object packet) {

    }

    public final void sync(BossBar bossBar) {
        for (UUID uuid : getPlayers(bossBar))
            add(uuid, bossBar);
    }

}
