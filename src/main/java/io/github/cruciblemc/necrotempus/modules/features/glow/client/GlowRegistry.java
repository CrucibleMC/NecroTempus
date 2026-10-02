package io.github.cruciblemc.necrotempus.modules.features.glow.client;

import cpw.mods.fml.common.FMLCommonHandler;

public interface GlowRegistry {

    static void init() {
        GlowClientManager manager = GlowClientManager.getInstance();
        manager.registerDebugKey();
        FMLCommonHandler.instance()
            .bus()
            .register(manager);
    }
}
