package io.github.cruciblemc.necrotempus.modules.features.bossbar.client;

import cpw.mods.fml.common.FMLCommonHandler;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.render.BossBarGui;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.render.BossDisplayAdapterListener;
import net.minecraftforge.common.MinecraftForge;

public interface BossBarRegistry {

    static void init() {
        MinecraftForge.EVENT_BUS.register(BossBarGui.getInstance());
        MinecraftForge.EVENT_BUS.register(BossDisplayAdapterListener.getInstance());
        FMLCommonHandler.instance().bus().register(ClientBossBarManager.getInstance());
    }

}
