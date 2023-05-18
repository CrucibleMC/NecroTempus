package io.github.crucible.necrotempus.modules.bossbar.internal.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.crucible.necrotempus.modules.bossbar.api.BossBarColor;
import io.github.crucible.necrotempus.modules.bossbar.api.BossBarType;
import io.github.crucible.necrotempus.modules.bossbar.api.BossBar;
import io.github.crucible.necrotempus.modules.bossbar.internal.manager.ClientBossBarManager;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;

public class BossDisplayWrapper {

    private static BossDisplayWrapper instance;

    public static BossDisplayWrapper getInstance() {
        return (instance != null) ? instance : new BossDisplayWrapper();
    }

    private BossDisplayWrapper(){
        instance = this;
    }

    @SubscribeEvent
    public void onRenderGameOverlayEvent(RenderGameOverlayEvent.Pre event){
        if(event.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre event) {

        if(event.entity instanceof IBossDisplayData){

            IBossDisplayData bossDisplayData = (IBossDisplayData) event.entity;

            BossBar bossBar = BossBar.createBossBar(event.entity.getUniqueID());

            bossBar.setText((ChatComponentText) bossDisplayData.func_145748_c_());
            bossBar.setPercentage(bossDisplayData.getHealth() / bossDisplayData.getMaxHealth());
            bossBar.setCreationTime(System.currentTimeMillis());

            if(event.entity instanceof EntityDragon){
                bossBar.setColor(BossBarColor.PURPLE);
                bossBar.setType(BossBarType.NOTCHED_20);
            }else if(event.entity instanceof EntityWither){
                bossBar.setColor(BossBarColor.RED);
                bossBar.setType(BossBarType.NOTCHED_20);
            }else {
                bossBar.setColor(BossBarColor.PINK);
                bossBar.setType(BossBarType.FLAT);
            }

            ClientBossBarManager.add(bossBar);
        }

    }

}
