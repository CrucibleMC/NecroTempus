package io.github.crucible.necrotempus.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import io.github.crucible.necrotempus.modules.bossbar.api.BossBar;
import io.github.crucible.necrotempus.modules.bossbar.internal.server.BossBarPlayerManagerElementServer;

public class ServerProxy implements IProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {

    }

    @Override
    public void init(FMLInitializationEvent event) {

    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {

    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        BossBar.BOSS_BAR_PLAYER_MANAGER_ELEMENT = new BossBarPlayerManagerElementServer();
    }
}
