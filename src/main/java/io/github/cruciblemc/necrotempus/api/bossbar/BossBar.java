package io.github.cruciblemc.necrotempus.api.bossbar;

import static io.github.cruciblemc.necrotempus.api.bossbar.BossBarManager.commonInstance;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import lombok.Getter;
import lombok.Setter;

public class BossBar extends BossBarComponent {

    @Getter
    @Setter
    private static BossBarManager BossBarManager = commonInstance();

    private BossBar() {
        super();
    }

    private BossBar(UUID uuid) {
        super(uuid);
    }

    private BossBar(NBTTagCompound tagCompound) {
        super(tagCompound);
    }

    private BossBar(UUID uuid, ChatComponentText text, BossBarColor color, BossBarType type, Float percent,
        boolean isVisible) {
        super(uuid, text, color, type, percent, isVisible);
    }

    public static BossBar createBossBar() {
        return new BossBar();
    }

    public static BossBar createBossBar(UUID uuid) {
        return new BossBar(uuid);
    }

    public static BossBar createBossBar(NBTTagCompound nbtTagCompound) {
        return new BossBar(nbtTagCompound);
    }

    public static BossBar createBossBar(UUID uuid, ChatComponentText text, BossBarColor color, BossBarType type,
        Float percent, boolean isVisible) {
        return new BossBar(uuid, text, color, type, percent, isVisible);
    }

}
