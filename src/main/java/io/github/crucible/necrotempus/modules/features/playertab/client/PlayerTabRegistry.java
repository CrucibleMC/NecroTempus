package io.github.crucible.necrotempus.modules.features.playertab.client;

import io.github.crucible.necrotempus.modules.features.playertab.client.render.PlayerTabDisplayListener;
import net.minecraftforge.common.MinecraftForge;

public interface PlayerTabRegistry {

    static void init(){
        MinecraftForge.EVENT_BUS.register(PlayerTabDisplayListener.getInstance());
    }

}
