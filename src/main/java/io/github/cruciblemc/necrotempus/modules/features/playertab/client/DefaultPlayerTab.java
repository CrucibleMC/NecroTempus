package io.github.cruciblemc.necrotempus.modules.features.playertab.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import org.apache.commons.lang3.StringUtils;

import com.mojang.authlib.GameProfile;

import io.github.cruciblemc.necrotempus.api.playertab.PlayerTab;
import io.github.cruciblemc.necrotempus.api.playertab.TabCell;
import io.github.cruciblemc.necrotempus.modules.features.playertab.client.render.PlayerTabGui;
import io.github.cruciblemc.necrotempus.utils.NetHandlerPlayClientNT;

public class DefaultPlayerTab extends PlayerTab {

    private static PlayerTab instance;
    private static Long lastCellsUpdate;
    private static List<TabCell> cachedList;

    public static PlayerTab getInstance() {
        return instance != null ? instance : new DefaultPlayerTab();
    }

    private DefaultPlayerTab() {
        super();
        instance = this;
    }

    @Override
    public IChatComponent getHeader() {
        return null;
    }

    @Override
    public IChatComponent getFooter() {
        return null;
    }

    @Override
    public List<TabCell> getCellList() {

        if (lastCellsUpdate != null) {

            long time = System.currentTimeMillis() - lastCellsUpdate;

            if (time <= 250 && cachedList != null) return cachedList;
        }

        lastCellsUpdate = System.currentTimeMillis();

        Minecraft minecraft = Minecraft.getMinecraft();

        List<TabCell> tabCells = new ArrayList<>();

        NetHandlerPlayClientNT handlerWrapper = NetHandlerPlayClientNT.of(minecraft.thePlayer.sendQueue);

        for (GuiPlayerInfo guiPlayerInfo : handlerWrapper.getServerPlayers()) {

            EntityPlayer entityPlayer = minecraft.theWorld.getPlayerEntityByName(guiPlayerInfo.name);

            String name = guiPlayerInfo.name;

            // Yes, sometimes can be blank.
            if (StringUtils.isBlank(name)) {
                continue;
            }

            GameProfile gameProfile = entityPlayer != null ? entityPlayer.getGameProfile()
                : new GameProfile(null, name);

            tabCells.add(
                new TabCell(
                    new ChatComponentText(PlayerTabGui.getFormattedPlayerName(guiPlayerInfo.name, minecraft)),
                    guiPlayerInfo.name,
                    gameProfile,
                    true,
                    guiPlayerInfo.responseTime));
        }

        Scoreboard worldScoreboard = minecraft.theWorld.getScoreboard();

        if (worldScoreboard != null) {

            tabCells.sort(Comparator.comparing(el -> {

                ScorePlayerTeam team = worldScoreboard.getPlayersTeam(el.getLinkedUserName());

                if (team == null) return "Z";

                return worldScoreboard.getPlayersTeam(el.getLinkedUserName())
                    .getRegisteredName();

            }));

        }

        cachedList = tabCells;
        return tabCells;
    }

    @Override
    public boolean isDrawPlayerHeads() {
        return true;
    }
}
