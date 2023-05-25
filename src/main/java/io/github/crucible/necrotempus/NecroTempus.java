package io.github.crucible.necrotempus;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.github.crucible.necrotempus.modules.bossbar.internal.network.BossBarPacket;
import io.github.crucible.necrotempus.modules.bossbar.internal.network.BossBarPacketHandler;
import io.github.crucible.necrotempus.modules.playertab.internal.PlayerTabManager;
import io.github.crucible.necrotempus.modules.playertab.internal.network.PlayerTabPacket;
import io.github.crucible.necrotempus.modules.playertab.internal.network.PlayerTabPacketHandler;
import io.github.crucible.necrotempus.proxy.CommonProxy;

import static io.github.crucible.necrotempus.Tags.MODID;

@Mod(modid = MODID, name = Tags.MODNAME, version = Tags.VERSION)
public class NecroTempus {


    @Mod.Instance(MODID)
    private static NecroTempus instance;

    public static NecroTempus getInstance() {
        return instance;
    }

    public NecroTempusAPI NECRO_TEMPUS_API;

    public NecroTempusAPI getNecroTempusApi() {
        return NECRO_TEMPUS_API;
    }

    @SidedProxy(
            clientSide="io.github.crucible.necrotempus.proxy.ClientProxy",
            serverSide="io.github.crucible.necrotempus.proxy.ServerProxy"
    )
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper DISPATCHER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event){
        NECRO_TEMPUS_API = createAPI();
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
        DISPATCHER.registerMessage(PlayerTabPacketHandler.class, PlayerTabPacket.class, 1, Side.CLIENT);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    private NecroTempusAPI createAPI(){
        return new NecroTempusAPI(
                new PlayerTabManager()
        );
    }
}
