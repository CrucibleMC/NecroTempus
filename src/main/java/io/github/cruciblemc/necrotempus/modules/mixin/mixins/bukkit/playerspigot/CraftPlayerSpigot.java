package io.github.cruciblemc.necrotempus.modules.mixin.mixins.bukkit.playerspigot;

import io.github.cruciblemc.necrotempus.api.actionbar.ActionBar;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.util.ChatComponentText;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

@Mixin(targets = "org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer$1", remap = false)
@Pseudo
public abstract class CraftPlayerSpigot {

    @Shadow
    CraftPlayer this$0;

    @Shadow
    public abstract void sendMessage(BaseComponent... component);

    /**
     * @author brunoxkk0
     * @reason Necro Tempus Crucible API overwrite.
     */
    @Overwrite
    public void sendMessage(net.md_5.bungee.api.ChatMessageType position, net.md_5.bungee.api.chat.BaseComponent component) {

        if (position == ChatMessageType.CHAT)
            sendMessage(component);

        if (position == ChatMessageType.ACTION_BAR) {
            ActionBar actionBar = new ActionBar(70, new ChatComponentText(component.toLegacyText()));
            UUID target = this$0.getUniqueId();
            ActionBar.getActionBarManager().set(new HashSet<>(Collections.singleton(target)), actionBar);
        }

    }

    /**
     * @author brunoxkk0
     * @reason Necro Tempus Crucible API overwrite.
     */
    @Overwrite
    public void sendMessage(net.md_5.bungee.api.ChatMessageType position, net.md_5.bungee.api.chat.BaseComponent... components) {

        if (position == ChatMessageType.CHAT)
            sendMessage(components);

        if (position == ChatMessageType.ACTION_BAR) {

            StringBuilder string = new StringBuilder();

            for (BaseComponent component : components) {
                string.append(component.toLegacyText());
            }

            ActionBar actionBar = new ActionBar(70, new ChatComponentText(string.toString()));
            UUID target = this$0.getUniqueId();
            ActionBar.getActionBarManager().set(new HashSet<>(Collections.singleton(target)), actionBar);
        }

    }
}
