package io.github.crucible.necrotempus;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.github.crucible.necrotempus.modules.bossbar.api.BossBar;
import io.github.crucible.necrotempus.modules.bossbar.api.BossBarColor;
import io.github.crucible.necrotempus.modules.bossbar.api.BossBarType;
import io.github.crucible.necrotempus.modules.bossbar.internal.network.BossBarPacket;
import io.github.crucible.necrotempus.modules.bossbar.internal.network.BossBarPacketHandler;
import io.github.crucible.necrotempus.proxy.IProxy;

import java.util.UUID;

@Mod(modid = "cruciblenecrotempus",name = "Crucible NecroTempus", version = "1.0")
public class CrucibleNecroTempus {

    public static final String MODID = "cruciblenecrotempus";

    @Mod.Instance(MODID)
    public static CrucibleNecroTempus instance;

    @SidedProxy(
            clientSide="io.github.crucible.necrotempus.proxy.ClientProxy",
            serverSide="io.github.crucible.necrotempus.proxy.ServerProxy"
    )
    public static IProxy proxy;

    public static SimpleNetworkWrapper DISPATCHER;

    public NecroTempusAPI NECRO_TEMPUS_API;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event){
        NECRO_TEMPUS_API = new NecroTempusAPI();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
        DISPATCHER = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        DISPATCHER.registerMessage(BossBarPacketHandler.class, BossBarPacket.class, 0, Side.CLIENT);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
        FMLCommonHandler.instance().bus().register(this);
    }
}
