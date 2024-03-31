package io.github.cruciblemc.necrotempus.proxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.compat.crafttweaker.BossBar;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.compat.crafttweaker.Glyphs;
import minetweaker.MineTweakerAPI;

import java.util.Arrays;

public abstract class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
        try {
            Arrays.asList(BossBar.class, Glyphs.class).forEach(MineTweakerAPI::registerClass);
        } catch (NoClassDefFoundError e) {
            NecroTempus.getInstance().getLogger().warn("CraftTweaker is not available.");
        }
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    public void serverStarting(FMLServerStartingEvent event) {
    }

}
