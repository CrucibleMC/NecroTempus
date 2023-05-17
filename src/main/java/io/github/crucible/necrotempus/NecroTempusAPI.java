package io.github.crucible.necrotempus;

import io.github.crucible.necrotempus.modules.bossbar.api.BossBarApi;

public class NecroTempusAPI {

    private final BossBarApi BOSS_BAR_API;

    public NecroTempusAPI(BossBarApi bossBarApi) {
        BOSS_BAR_API = bossBarApi;
    }

    public BossBarApi getBossBarApi() {
        return BOSS_BAR_API;
    }
}
