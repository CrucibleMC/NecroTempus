package io.github.cruciblemc.necrotempus.modules.features.actionbar.client;

import net.minecraftforge.common.MinecraftForge;

public interface ActionBarRegistry {

    static void init() {
        MinecraftForge.EVENT_BUS.register(ClientGuiIngameModifier.getInstance());
    }

}
