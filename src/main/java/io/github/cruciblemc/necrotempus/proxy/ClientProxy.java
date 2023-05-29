package io.github.cruciblemc.necrotempus.proxy;

import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import io.github.cruciblemc.necrotempus.modules.features.actionbar.client.ActionBarRegistry;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.BossBarRegistry;
import io.github.cruciblemc.necrotempus.modules.features.playertab.client.PlayerTabRegistry;
import io.github.cruciblemc.necrotempus.modules.features.title.client.TitleRegistry;

public class ClientProxy extends CommonProxy {

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        BossBarRegistry.init();
        PlayerTabRegistry.init();
        TitleRegistry.init();
        ActionBarRegistry.init();
    }

}
