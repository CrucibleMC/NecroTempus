package io.github.cruciblemc.necrotempus.modules.features.title.client;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import io.github.cruciblemc.necrotempus.modules.features.title.client.render.TitleDisplayListener;

public interface TitleRegistry {

    static void init() {
        MinecraftForge.EVENT_BUS.register(TitleDisplayListener.getInstance());
        FMLCommonHandler.instance()
            .bus()
            .register(ClientTitleManager.getInstance());
    }

}
