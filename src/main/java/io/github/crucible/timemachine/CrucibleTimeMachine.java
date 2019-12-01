package io.github.crucible.timemachine;

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
import io.github.crucible.timemachine.bossbar.BossBarColor;
import io.github.crucible.timemachine.bossbar.BossBarType;
import io.github.crucible.timemachine.bossbar.network.BossBarPacket;
import io.github.crucible.timemachine.bossbar.network.BossBarPacketHandler;
import io.github.crucible.timemachine.bossbar.server.BossBar;
import io.github.crucible.timemachine.proxy.IProxy;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import java.util.Random;
import java.util.UUID;

@Mod(modid = "crucibletimemachine",name = "Crucible TimeMachine", version = "1.0")
public class CrucibleTimeMachine {

    @Mod.Instance("crucibletimemachine")
    public static CrucibleTimeMachine instance;

    @SidedProxy(
            clientSide="io.github.crucible.timemachine.proxy.ClientProxy",
            serverSide="io.github.crucible.timemachine.proxy.ServerProxy"
    )
    public static IProxy proxy;
    private static SimpleNetworkWrapper dispatcher;


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
        dispatcher = NetworkRegistry.INSTANCE.newSimpleChannel("crucibletimemachine");
        dispatcher.registerMessage(BossBarPacketHandler.class, BossBarPacket.class, 0, Side.CLIENT);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
        FMLCommonHandler.instance().bus().register(this);
    }



    @SubscribeEvent
    public void onJoin(PlayerEvent.PlayerLoggedInEvent event){
        BossBarPacket packet = new BossBarPacket(new BossBar(new ChatComponentText(new Random().nextLong() + ""), BossBarColor.WHITE, BossBarType.NOTCHED_12,15F,true, UUID.randomUUID()),BossBarPacket.PacketType.ADD);
        dispatcher.sendTo(packet, (EntityPlayerMP) event.player);
    }

}
