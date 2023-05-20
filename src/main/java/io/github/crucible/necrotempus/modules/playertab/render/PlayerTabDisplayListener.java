package io.github.crucible.necrotempus.modules.playertab.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.crucible.necrotempus.CrucibleNecroTempus;
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
        if(event.type == RenderGameOverlayEvent.ElementType.PLAYER_LIST && CrucibleNecroTempus.getInstance().getnecroTempusApi().isCustomServerTabEnabled()){
            event.setCanceled(true);
            if ( PlayerTabGui.getInstance().shouldRender()){
                PlayerTabGui.getInstance().render(event.resolution.getScaledWidth(), CrucibleNecroTempus.getInstance().getnecroTempusApi().getPlayerTabManager());
            }
        }
    }

}
