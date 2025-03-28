package io.github.cruciblemc.necrotempus;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.github.cruciblemc.necrotempus.modules.features.actionbar.network.ActionBarPacket;
import io.github.cruciblemc.necrotempus.modules.features.actionbar.network.ActionBarPacketHandler;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.network.BossBarPacket;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.network.BossBarPacketHandler;
import io.github.cruciblemc.necrotempus.modules.features.core.ClientResetState;
import io.github.cruciblemc.necrotempus.modules.features.packet.NTClientPacket;
import io.github.cruciblemc.necrotempus.modules.features.packet.NTClientPacketHandler;
import io.github.cruciblemc.necrotempus.modules.features.playertab.network.PlayerTabPacket;
import io.github.cruciblemc.necrotempus.modules.features.playertab.network.PlayerTabPacketHandler;
import io.github.cruciblemc.necrotempus.modules.features.title.network.TitlePacket;
import io.github.cruciblemc.necrotempus.modules.features.title.network.TitlePacketHandler;
import io.github.cruciblemc.necrotempus.proxy.CommonProxy;
import io.github.cruciblemc.omniconfig.api.OmniconfigAPI;
import lombok.Getter;
import net.minecraft.network.NetHandlerPlayServer;
import org.apache.logging.log4j.Logger;


@Mod(modid = Tags.MODID, name = Tags.MODNAME, version = Tags.VERSION, dependencies = "required-after:Omniconfig; after:SkinPort")
public class NecroTempus {

    @Mod.Instance(Tags.MODID)
    @Getter
    private static NecroTempus instance;

    @SidedProxy(
            clientSide = "io.github.cruciblemc.necrotempus.proxy.ClientProxy",
            serverSide = "io.github.cruciblemc.necrotempus.proxy.ServerProxy"
    )
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper DISPATCHER = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MODID + ":main");

    @Getter
    public Logger logger;

    static {
        DISPATCHER.registerMessage(NTClientPacketHandler.class, NTClientPacket.class, 0, Side.SERVER);
        DISPATCHER.registerMessage(BossBarPacketHandler.class, BossBarPacket.class, 1, Side.CLIENT);
        DISPATCHER.registerMessage(PlayerTabPacketHandler.class, PlayerTabPacket.class, 2, Side.CLIENT);
        DISPATCHER.registerMessage(TitlePacketHandler.class, TitlePacket.class, 3, Side.CLIENT);
        DISPATCHER.registerMessage(ActionBarPacketHandler.class, ActionBarPacket.class, 4, Side.CLIENT);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        OmniconfigAPI.registerAnnotationConfig(NecroTempusConfig.class);
        proxy.preInit(event);
        FMLCommonHandler.instance().bus().register(this);
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

    @SuppressWarnings("rawtypes")
    @SubscribeEvent
    public void onNetworkRegister(FMLNetworkEvent.CustomPacketRegistrationEvent event) {

        if (event.operation.equals("REGISTER")) {

            boolean hasNecroTempus = event.registrations.contains(Tags.MODID + ":main");

            if (!(event.handler instanceof NetHandlerPlayServer) && !hasNecroTempus) {
                NecroTempus.getInstance().getLogger().info("Connected to a server that does not have NecroTempus, resetting client managers.");
                ClientResetState.resetRender();
            }

        }

    }

}
