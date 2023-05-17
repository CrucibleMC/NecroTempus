package io.github.crucible.necrotempus.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import io.github.crucible.necrotempus.modules.bossbar.internal.render.BossBarGui;
import io.github.crucible.necrotempus.modules.bossbar.internal.render.BossDisplayWrapper;
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
        MinecraftForge.EVENT_BUS.register(BossBarGui.getInstance());
        MinecraftForge.EVENT_BUS.register(BossDisplayWrapper.getInstance());
    }

    @Override
    public void serverStarting(FMLServerStartingEvent event) {

    }
}
