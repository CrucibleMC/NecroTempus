package io.github.cruciblemc.necrotempus.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import io.github.cruciblemc.necrotempus.modules.features.compat.MineTweaker;

public abstract class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
        MineTweaker.init();
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    public void serverStarting(FMLServerStartingEvent event) {
    }

}
