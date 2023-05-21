package io.github.crucible.necrotempus.modules.playertab.client;

import io.github.crucible.necrotempus.modules.playertab.render.PlayerTabDisplayListener;
import net.minecraftforge.common.MinecraftForge;

public interface PlayerTabRegistry {

    static void init(){
        MinecraftForge.EVENT_BUS.register(PlayerTabDisplayListener.getInstance());
    }

}
