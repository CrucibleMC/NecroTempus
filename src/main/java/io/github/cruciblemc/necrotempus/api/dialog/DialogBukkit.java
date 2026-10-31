package io.github.cruciblemc.necrotempus.api.dialog;

import java.util.function.BiConsumer;

import net.minecraft.entity.player.EntityPlayerMP;

import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class DialogBukkit {

    private DialogBukkit() {}

    public static DialogManager.ShowResult show(Player player, Dialog dialog,
        BiConsumer<EntityPlayerMP, DialogResponse> callback) {
        if (!(player instanceof CraftPlayer)) return DialogManager.ShowResult.INVALID_PLAYER;
        return DialogManager.show(((CraftPlayer) player).getHandle(), dialog, callback);
    }

    public static void clear(Player player) {
        if (player instanceof CraftPlayer) DialogManager.clear(((CraftPlayer) player).getHandle());
    }
}
