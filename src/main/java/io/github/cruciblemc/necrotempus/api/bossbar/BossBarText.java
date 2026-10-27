package io.github.cruciblemc.necrotempus.api.bossbar;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public final class BossBarText {

    public static ChatComponentText fromDisplayName(IChatComponent displayName) {
        return new ChatComponentText(displayName.getFormattedText());
    }

    private BossBarText() {}
}
