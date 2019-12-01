package io.github.crucible.timemachine;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.crucible.timemachine.bossbar.client.BossBarGui;
import io.github.crucible.timemachine.bossbar.server.BossBar;
import io.github.crucible.timemachine.proxy.IProxy;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

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
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    private BossBarGui gui = new BossBarGui(Minecraft.getMinecraft());
    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.EXPERIENCE) return;
        gui.render();
    }
}
