package io.github.cruciblemc.necrotempus.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import io.github.crucible.api.CrucibleAPI;
import io.github.cruciblemc.necrotempus.NecroTempusPlugin;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBar;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.server.BossBarManagerServer;
import io.github.cruciblemc.necrotempus.utils.CrucibleAsk;

public class ServerProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        BossBar.setBossBarManager(new BossBarManagerServer());

        if (CrucibleAsk.isAvailable()) {
            CrucibleAPI.registerModPlugin(new NecroTempusPlugin());
        }

    }
}
