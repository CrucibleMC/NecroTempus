package io.github.crucible.necrotempus.modules.bossbar.api;

import io.github.crucible.necrotempus.modules.bossbar.internal.api.BossBar;
import io.github.crucible.necrotempus.modules.bossbar.internal.api.BossBarColor;
import io.github.crucible.necrotempus.modules.bossbar.internal.api.BossBarType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import java.util.UUID;

public interface BossBarApi {

    BossBar createBossBar();

    BossBar createBossBar(UUID uuid);

    BossBar createBossBar(NBTTagCompound nbtTagCompound);

    BossBar createBossBar(UUID uuid, ChatComponentText text, BossBarColor color, BossBarType type, Float percent, boolean isVisible);

    void add(BossBar bossBar);

    void update(BossBar bossBar);

    void remove(BossBar bossBar);

}
