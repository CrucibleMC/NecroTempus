package io.github.crucible.timemachine.bossbar.server;


import com.google.common.collect.Sets;

import java.util.*;

public class BossBarHandler {

    private static Set<BossBar> currentBossBars = Sets.newHashSet();

    public boolean addBossBar(BossBar bossBar){

        if(getBossBar(bossBar.getUuid()) != null){
            return false;
        }

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

    public Set<UUID> getPlayers(UUID bossbar){
        BossBar bossBar;

        if((bossBar = getBossBar(bossbar)) != null){
            return bossBar.getPlayers();
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
