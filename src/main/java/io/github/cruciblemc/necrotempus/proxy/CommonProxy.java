package io.github.cruciblemc.necrotempus.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public abstract class CommonProxy {

    public void preInit(FMLPreInitializationEvent event){}

    public void init(FMLInitializationEvent event){}

    public void postInit(FMLPostInitializationEvent event){}

    public void serverStarting(FMLServerStartingEvent event){}

}
