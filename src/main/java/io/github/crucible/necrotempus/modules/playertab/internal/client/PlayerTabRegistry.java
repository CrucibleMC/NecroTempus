package io.github.crucible.necrotempus.modules.playertab.internal.client;

import io.github.crucible.necrotempus.modules.playertab.internal.render.PlayerTabDisplayListener;
import net.minecraftforge.common.MinecraftForge;

public interface PlayerTabRegistry {

    static void init(){
        MinecraftForge.EVENT_BUS.register(PlayerTabDisplayListener.getInstance());
    }

}
