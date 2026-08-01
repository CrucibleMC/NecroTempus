package io.github.cruciblemc.necrotempus.modules.features.playertab.client;

import net.minecraftforge.common.MinecraftForge;

import io.github.cruciblemc.necrotempus.modules.features.playertab.client.render.PlayerTabDisplayListener;

public interface PlayerTabRegistry {

    static void init() {
        MinecraftForge.EVENT_BUS.register(PlayerTabDisplayListener.getInstance());
    }

}
