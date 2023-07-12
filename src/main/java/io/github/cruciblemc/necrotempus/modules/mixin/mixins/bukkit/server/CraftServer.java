package io.github.cruciblemc.necrotempus.modules.mixin.mixins.bukkit.server;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_7_R4.boss.CraftBossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = org.bukkit.craftbukkit.v1_7_R4.CraftServer.class, remap = false)
public abstract class CraftServer {

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public BossBar createBossBar(String title, BarColor color, BarStyle style, BarFlag... flags) {
        return (BossBar) new CraftBossBar(title, color, style, flags);
    }

}
