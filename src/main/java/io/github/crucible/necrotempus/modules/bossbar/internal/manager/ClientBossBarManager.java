package io.github.crucible.necrotempus.modules.bossbar.internal.manager;

import io.github.crucible.necrotempus.modules.bossbar.internal.api.BossBar;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.UUID;

public class ClientBossBarManager {

    private ClientBossBarManager(){}

    private static final LinkedHashMap<UUID, BossBar> BOSS_BARS_ENTRIES = new LinkedHashMap<>();

    public static void add(BossBar bossBar){
        BOSS_BARS_ENTRIES.put(bossBar.getUuid(), bossBar);
    }

    public static void remove(BossBar bossBar){
        BOSS_BARS_ENTRIES.remove(bossBar.getUuid());
    }

    public static Iterator<BossBar> iterator() {
        return BOSS_BARS_ENTRIES.values().iterator();
    }

    public static boolean isEmpty(){
        return BOSS_BARS_ENTRIES.isEmpty();
    }
}
