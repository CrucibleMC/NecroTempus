package io.github.crucible.necrotempus.proxy;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import io.github.crucible.necrotempus.modules.bossbar.internal.manager.ClientBossBarManager;
import io.github.crucible.necrotempus.modules.bossbar.internal.render.BossBarGui;
import io.github.crucible.necrotempus.modules.bossbar.internal.render.BossDisplayWrapper;
import io.github.crucible.necrotempus.modules.playertab.render.PlayerTabDisplayListener;
import io.github.crucible.necrotempus.modules.playertab.render.PlayerTabGui;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy implements IProxy{

    @Override
    public void preInit(FMLPreInitializationEvent event) {

    }

    @Override
    public void init(FMLInitializationEvent event) {

    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        {
            MinecraftForge.EVENT_BUS.register(BossBarGui.getInstance());
            MinecraftForge.EVENT_BUS.register(BossDisplayWrapper.getInstance());
            FMLCommonHandler.instance().bus().register(ClientBossBarManager.getInstance());
        }
        {
            MinecraftForge.EVENT_BUS.register(PlayerTabDisplayListener.getInstance());
        }
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {

    }
}
