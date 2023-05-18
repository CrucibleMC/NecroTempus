package io.github.crucible.necrotempus.modules.bossbar.internal.component;

import io.github.crucible.necrotempus.modules.bossbar.api.BossBar;
import io.github.crucible.necrotempus.modules.bossbar.internal.network.BossBarPacket;

import java.util.Set;
import java.util.UUID;

public interface BossBarPlayerManagerElement {

    void add(UUID target, BossBar bossBar);

    void remove(BossBar bossBar);

    void remove(UUID target, BossBar bossBar);

    void deliver(Set<UUID> players, BossBarPacket packet);

    static BossBarPlayerManagerElement commonInstance(){
        return new BossBarPlayerManagerElement() {

            @Override
            public void add(UUID target, BossBar bossBar) {}

            @Override
            public void remove(BossBar bossBar) {}

            @Override
            public void remove(UUID target, BossBar bossBar) {}

            @Override
            public void deliver(Set<UUID> players, BossBarPacket packet) {}

        };
    }

}
