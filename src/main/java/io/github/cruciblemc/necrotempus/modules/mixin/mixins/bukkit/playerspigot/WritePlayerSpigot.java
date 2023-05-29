package io.github.cruciblemc.necrotempus.modules.mixin.mixins.bukkit.playerspigot;

import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Player.Spigot.class, remap = false)
public class WritePlayerSpigot {

    /**
     * Sends the component to the specified screen position of this player
     *
     * @param position the screen position
     * @param component the components to send
     */
    @Intrinsic
    public void sendMessage(net.md_5.bungee.api.ChatMessageType position, net.md_5.bungee.api.chat.BaseComponent component) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Sends an array of components as a single message to the specified screen position of this player
     *
     * @param position the screen position
     * @param components the components to send
     */
    @Intrinsic
    public void sendMessage(net.md_5.bungee.api.ChatMessageType position, net.md_5.bungee.api.chat.BaseComponent... components) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
