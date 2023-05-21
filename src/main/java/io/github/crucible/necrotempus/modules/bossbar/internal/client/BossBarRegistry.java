package io.github.crucible.necrotempus.modules.bossbar.internal.client;

import cpw.mods.fml.common.FMLCommonHandler;
import io.github.crucible.necrotempus.modules.bossbar.internal.manager.ClientBossBarManager;
import io.github.crucible.necrotempus.modules.bossbar.internal.render.BossBarGui;
import io.github.crucible.necrotempus.modules.bossbar.internal.render.BossDisplayAdapterListener;
import net.minecraftforge.common.MinecraftForge;

public interface BossBarRegistry {

    static void init(){
        MinecraftForge.EVENT_BUS.register(BossBarGui.getInstance());
        MinecraftForge.EVENT_BUS.register(BossDisplayAdapterListener.getInstance());
        FMLCommonHandler.instance().bus().register(ClientBossBarManager.getInstance());
    }

}
