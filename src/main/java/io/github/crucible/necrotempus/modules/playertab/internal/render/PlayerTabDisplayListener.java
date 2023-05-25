package io.github.crucible.necrotempus.modules.playertab.internal.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.crucible.necrotempus.NecroTempus;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public class PlayerTabDisplayListener {

    private static PlayerTabDisplayListener instance;

    public static PlayerTabDisplayListener getInstance() {
        return (instance != null) ? instance : new PlayerTabDisplayListener();
    }

    private PlayerTabDisplayListener(){
        instance = this;
    }

    @SubscribeEvent
    public void onRenderGameOverlayEvent(RenderGameOverlayEvent.Pre event){
        if(NecroTempus.getInstance().getNecroTempusApi().isCustomServerTabEnabled()){
            if(event.type == RenderGameOverlayEvent.ElementType.PLAYER_LIST)
                event.setCanceled(true);
            if(event.type == RenderGameOverlayEvent.ElementType.EXPERIENCE && NecroTempus.getInstance().getNecroTempusApi().isCustomServerTabEnabled()){
                if ( PlayerTabGui.getInstance().shouldRender()){
                    PlayerTabGui.getInstance().render(event.resolution.getScaledWidth(), NecroTempus.getInstance().getNecroTempusApi().getPlayerTabManager());
                }
            }
        }
    }

}
