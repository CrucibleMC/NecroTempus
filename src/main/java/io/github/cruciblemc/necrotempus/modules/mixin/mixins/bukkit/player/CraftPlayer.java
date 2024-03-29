package io.github.cruciblemc.necrotempus.modules.mixin.mixins.bukkit.player;

import io.github.cruciblemc.necrotempus.api.playertab.PlayerTab;
import io.github.cruciblemc.necrotempus.api.title.TitleComponent;
import io.github.cruciblemc.necrotempus.api.title.TitleElement;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

@Pseudo
@Mixin(targets = "org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer", remap = false)
public abstract class CraftPlayer {

    @Shadow
    public abstract EntityPlayerMP getHandle();

    @Shadow
    public abstract Player getPlayer();

    @Shadow
    public abstract boolean equals(Object o);

    @Shadow
    public String playerListHeaderString;

    @Shadow
    public String playerListFooterString;

    /**
     * @author brunoxkk0
     * @reason Necro Tempus Crucible API overwrite.
     */
    @Deprecated
    @Overwrite
    public void sendTitle(String title, String subtitle) {
        sendTitle(title, subtitle, 10, 70, 20);
    }

    /**
     * @author brunoxkk0
     * @reason Necro Tempus Crucible API overwrite.
     */
    @Overwrite
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {

        if (title == null && subtitle == null) return;

        TitleComponent titleComponent = new TitleComponent();

        if (title != null)
            titleComponent.addElement(TitleElement.titleOf(new ChatComponentText(title)));

        if (subtitle != null)
            titleComponent.addElement(TitleElement.subtitleOf(new ChatComponentText(subtitle)));

        titleComponent.setFadeIn(fadeIn * 50);
        titleComponent.setStay(stay * 50);
        titleComponent.setFadeOut(fadeOut * 50);

        TitleComponent.getTitleManager().set(new HashSet<>(Collections.singleton(getHandle().getUniqueID())), titleComponent);

    }

    /**
     * @author brunoxkk0
     * @reason Necro Tempus Crucible API overwrite.
     */
    @Overwrite
    public void resetTitle() {
        TitleComponent.getTitleManager().set(new HashSet<>(Collections.singleton(getHandle().getUniqueID())), new TitleComponent());
    }

    /**
     * @author brunoxkk0
     * @reason Necro Tempus Crucible API overwrite.
     */
    @Overwrite
    public void setPlayerListHeaderFooter(@Nullable String header, @Nullable String footer) {

        PlayerTab playerTab = new PlayerTab();

        if ((playerListHeaderString = header) != null)
            playerTab.setHeader(new ChatComponentText(playerListHeaderString));

        if ((playerListFooterString = footer) != null)
            playerTab.setFooter(new ChatComponentText(playerListFooterString));

        playerTab.setDrawPlayerHeads(true);
        playerTab.setCellList(new ArrayList<>());

        PlayerTab.getPlayerTabManager().set(new HashSet<>(Collections.singleton(getHandle().getUniqueID())), playerTab);
    }


}