package io.github.crucible.timemachine.bossbar.server;

import io.github.crucible.timemachine.bossbar.BossBarColor;
import io.github.crucible.timemachine.bossbar.BossBarType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import java.util.UUID;

public class BossBarTemp extends BossBar{

    private final long startTime = System.currentTimeMillis();

    public BossBarTemp(UUID uuid) {
        super(uuid);
    }

    public BossBarTemp(NBTTagCompound tagCompound){
        super(tagCompound);
    }

    public BossBarTemp(ChatComponentText text, BossBarColor color, BossBarType type, Float percent, boolean isVisible, UUID uuid){
        super(text, color, type, percent, isVisible, uuid);
    }

    public long getStartTime() {
        return startTime;
    }
}
