package io.github.crucible.necrotempus.proxy;

import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import io.github.crucible.necrotempus.modules.bossbar.internal.client.BossBarRegistry;
import io.github.crucible.necrotempus.modules.playertab.internal.client.PlayerTabRegistry;

public class ClientProxy extends CommonProxy {

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        BossBarRegistry.init();
        PlayerTabRegistry.init();
    }

}
