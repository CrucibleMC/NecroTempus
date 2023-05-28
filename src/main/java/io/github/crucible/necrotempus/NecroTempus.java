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
import io.github.crucible.necrotempus.modules.features.actionbar.network.ActionBarPacket;
import io.github.crucible.necrotempus.modules.features.actionbar.network.ActionBarPacketHandler;
import io.github.crucible.necrotempus.modules.features.bossbar.network.BossBarPacket;
import io.github.crucible.necrotempus.modules.features.bossbar.network.BossBarPacketHandler;
import io.github.crucible.necrotempus.modules.features.playertab.network.PlayerTabPacket;
import io.github.crucible.necrotempus.modules.features.playertab.network.PlayerTabPacketHandler;
import io.github.crucible.necrotempus.modules.features.title.network.TitlePacket;
import io.github.crucible.necrotempus.modules.features.title.network.TitlePacketHandler;
import io.github.crucible.necrotempus.proxy.CommonProxy;

import static io.github.crucible.necrotempus.Tags.MODID;

@Mod(modid = MODID, name = Tags.MODNAME, version = Tags.VERSION)
public class NecroTempus {

    @Mod.Instance(MODID)
    private static NecroTempus instance;

    public static NecroTempus getInstance() {
        return instance;
    }

    @SidedProxy(
            clientSide="io.github.crucible.necrotempus.proxy.ClientProxy",
            serverSide="io.github.crucible.necrotempus.proxy.ServerProxy"
    )
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper DISPATCHER;

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
        DISPATCHER = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        DISPATCHER.registerMessage(BossBarPacketHandler.class,      BossBarPacket.class,    0, Side.CLIENT);
        DISPATCHER.registerMessage(PlayerTabPacketHandler.class,    PlayerTabPacket.class,  1, Side.CLIENT);
        DISPATCHER.registerMessage(TitlePacketHandler.class,        TitlePacket.class,      2, Side.CLIENT);
        DISPATCHER.registerMessage(ActionBarPacketHandler.class,    ActionBarPacket.class,  3, Side.CLIENT);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

}
