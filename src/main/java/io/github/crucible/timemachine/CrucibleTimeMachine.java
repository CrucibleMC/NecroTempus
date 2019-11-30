package io.github.crucible.timemachine;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import io.github.crucible.timemachine.proxy.IProxy;

@Mod(modid = "crucibletimemachine",name = "Crucible TimeMachine", version = "1.0")
public class CrucibleTimeMachine {

    @Mod.Instance("crucibletimemachine")
    public static CrucibleTimeMachine instance;

    @SidedProxy(
            clientSide="io.github.crucible.timemachine.proxy.ClientProxy",
            serverSide="io.github.crucible.timemachine.proxy.ServerProxy"
    )
    public static IProxy proxy;


    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event){
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
