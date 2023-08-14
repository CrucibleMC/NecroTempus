package io.github.cruciblemc.necrotempus;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.github.cruciblemc.necrotempus.modules.features.actionbar.network.ActionBarPacket;
import io.github.cruciblemc.necrotempus.modules.features.actionbar.network.ActionBarPacketHandler;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.network.BossBarPacket;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.network.BossBarPacketHandler;
import io.github.cruciblemc.necrotempus.modules.features.playertab.network.PlayerTabPacket;
import io.github.cruciblemc.necrotempus.modules.features.playertab.network.PlayerTabPacketHandler;
import io.github.cruciblemc.necrotempus.modules.features.title.network.TitlePacket;
import io.github.cruciblemc.necrotempus.modules.features.title.network.TitlePacketHandler;
import io.github.cruciblemc.necrotempus.proxy.CommonProxy;
import lombok.Getter;
import org.apache.logging.log4j.Logger;


@Mod(modid = Tags.MODID, name = Tags.MODNAME, version = Tags.VERSION)
public class NecroTempus {

    @Mod.Instance(Tags.MODID)
    private static NecroTempus instance;

    public static NecroTempus getInstance() {
        return instance;
    }

    @SidedProxy(
            clientSide="io.github.cruciblemc.necrotempus.proxy.ClientProxy",
            serverSide="io.github.cruciblemc.necrotempus.proxy.ServerProxy"
    )
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper DISPATCHER;

    @Getter
    public Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event){
        logger = event.getModLog();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
        DISPATCHER = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MODID + ":main");
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
