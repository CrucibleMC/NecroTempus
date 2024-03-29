package io.github.cruciblemc.necrotempus.modules.features.bossbar.client.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBar;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarColor;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarType;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.ClientBossBarManager;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.component.BossDisplayAdapter;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;

import java.util.LinkedHashSet;

public class BossDisplayAdapterListener {

    private static BossDisplayAdapterListener instance;

    public static BossDisplayAdapterListener getInstance() {
        return (instance != null) ? instance : new BossDisplayAdapterListener();
    }

    private BossDisplayAdapterListener() {
        instance = this;
    }

    private static final LinkedHashSet<BossDisplayAdapter> CUSTOM_ADAPTERS = new LinkedHashSet<>(BossDisplayAdapter.defaultList());

    public static void add(BossDisplayAdapter bossDisplayAdapter) {
        CUSTOM_ADAPTERS.add(bossDisplayAdapter);
    }

    public static void remove(BossDisplayAdapter bossDisplayAdapter) {
        CUSTOM_ADAPTERS.remove(bossDisplayAdapter);
    }


    @SubscribeEvent
    public void onRenderGameOverlayEvent(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre event) {

        if (event.entity instanceof IBossDisplayData bossDisplayData) {

            BossBar bossBar = BossBar.createBossBar(event.entity.getUniqueID());

            bossBar.setText((ChatComponentText) bossDisplayData.func_145748_c_());
            bossBar.setPercentage(bossDisplayData.getHealth() / bossDisplayData.getMaxHealth());
            bossBar.setCreationTime(System.currentTimeMillis());

            boolean customized = false;

            for (BossDisplayAdapter adapter : CUSTOM_ADAPTERS) {
                if (adapter.getTargetClass().equals(event.entity.getClass().getName())) {

                    bossBar.setColor(adapter.getColor());

                    if (adapter.getLazyColor() != -1) {
                        bossBar.setLazyColor(adapter.getLazyColor());
                    }

                    bossBar.setType(adapter.getType());
                    customized = true;
                    break;
                }
            }

            if (!customized) {
                bossBar.setColor(BossBarColor.PINK);
                bossBar.setType(BossBarType.FLAT);
            }

            if (bossBar.getType() != BossBarType.NONE)
                ClientBossBarManager.add(bossBar);
        }

    }

}
