package io.github.crucible.timemachine.bossbar.server;


import com.google.common.collect.Sets;

import java.util.*;

public class BossBarHandler {

    private static final HashSet<BossBar> currentBossBars = Sets.newHashSet();

    public boolean addBossBar(BossBar bossBar){
        return currentBossBars.add(bossBar);
    }

    public boolean removeBossBar(UUID bossBar){

        BossBar bar;

        if((bar = getBossBar(bossBar)) != null){
            bar.clear();
        }

        return currentBossBars.remove(bar);

    }

    public Set<BossBar> getAllBars(){
        return currentBossBars;
    }

    public Set<UUID> getPlayers(UUID bossBar){
        BossBar bb;

        if((bb = getBossBar(bossBar)) != null){
            return bb.getPlayers();
        }

        return null;
    }

    public BossBar getBossBar(UUID bossbar){

        for(BossBar bossBar : currentBossBars){
            if(bossBar.getUuid().equals(bossbar)){
                return bossBar;
            }
        }

        return null;
    }

}
