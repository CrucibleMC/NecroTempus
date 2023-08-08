package io.github.cruciblemc.necrotempus.modules.features.bossbar.compat;

import minetweaker.MineTweakerAPI;

public class ZenRegister {
    public static void register() {
        MineTweakerAPI.registerClass(CraftTweakerBossBar.class);
    }
}
