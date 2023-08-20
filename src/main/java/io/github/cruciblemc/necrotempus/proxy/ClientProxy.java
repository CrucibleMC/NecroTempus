package io.github.cruciblemc.necrotempus.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import io.github.cruciblemc.necrotempus.modules.features.actionbar.client.ActionBarRegistry;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.BossBarRegistry;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.GlyphsRegistry;
import io.github.cruciblemc.necrotempus.modules.features.packet.NTClientPacketRegistry;
import io.github.cruciblemc.necrotempus.modules.features.playertab.client.PlayerTabRegistry;
import io.github.cruciblemc.necrotempus.modules.features.title.client.TitleRegistry;

public class ClientProxy extends CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        GlyphsRegistry.init();
    }

    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        BossBarRegistry.init();
        PlayerTabRegistry.init();
        TitleRegistry.init();
        ActionBarRegistry.init();
        NTClientPacketRegistry.init();
    }

}
