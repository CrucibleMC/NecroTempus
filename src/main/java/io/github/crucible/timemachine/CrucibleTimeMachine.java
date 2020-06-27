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
import io.github.crucible.timemachine.bossbar.BossBarAPI;
import io.github.crucible.timemachine.bossbar.BossBarColor;
import io.github.crucible.timemachine.bossbar.BossBarType;
import io.github.crucible.timemachine.bossbar.network.BossBarPacket;
import io.github.crucible.timemachine.bossbar.network.BossBarPacketHandler;
import io.github.crucible.timemachine.bossbar.server.BossBar;
import io.github.crucible.timemachine.proxy.IProxy;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import java.util.Arrays;
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

        BossBarAPI.READY = true;
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
        FMLCommonHandler.instance().bus().register(this);
    }



    //Teste Code

    @SubscribeEvent
    public void onJoin(PlayerEvent.PlayerLoggedInEvent event){
//
//        new Thread(){
//            public void run(){
//                Random random = new Random();
//                BossBar bar = new BossBar(new ChatComponentText(random.nextLong() + ""), BossBarColor.WHITE, BossBarType.NOTCHED_12,15F,true, UUID.randomUUID());
//                Enum[] colors = new Enum[]{BossBarColor.PINK, BossBarColor.BLUE, BossBarColor.GREEN,BossBarColor.PURPLE,BossBarColor.RED, BossBarColor.WHITE, BossBarColor.YELLOW};
//                Enum[] types = new Enum[]{BossBarType.FLAT,BossBarType.NOTCHED_6,BossBarType.NOTCHED_10,BossBarType.NOTCHED_12,BossBarType.NOTCHED_20};
//
//                while(true){
//
//                    bar.setText(new ChatComponentText(random.nextLong() + ""));
//                    bar.setColor((BossBarColor) Arrays.asList(colors).get(random.nextInt(colors.length)));
//                    bar.setType((BossBarType) Arrays.asList(types).get(random.nextInt(types.length)));
//                    bar.setPercent(random.nextInt(101));
//
//                    dispatcher.sendTo(new BossBarPacket(bar, BossBarPacket.PacketType.ADD), (EntityPlayerMP) event.player);
//
//                    try {
//                        sleep(400);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }.start();

    }


}
