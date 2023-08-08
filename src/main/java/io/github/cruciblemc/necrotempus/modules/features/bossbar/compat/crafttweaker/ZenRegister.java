package io.github.cruciblemc.necrotempus.modules.features.bossbar.compat.crafttweaker;

import minetweaker.MineTweakerAPI;

public class ZenRegister {

    public static void register() {
        MineTweakerAPI.registerClass(BossBar.class);
    }

}
