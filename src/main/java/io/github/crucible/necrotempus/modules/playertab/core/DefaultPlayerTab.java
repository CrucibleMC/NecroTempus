package io.github.crucible.necrotempus.modules.playertab.core;

import io.github.crucible.necrotempus.modules.playertab.component.PlayerTab;
import io.github.crucible.necrotempus.modules.playertab.component.TabCell;
import io.github.crucible.necrotempus.utils.NetHandlerPlayClientWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.List;

import static io.github.crucible.necrotempus.modules.playertab.render.PlayerTabGui.getFormattedPlayerName;

public class DefaultPlayerTab extends PlayerTab {

    private static PlayerTab instance;

    public static PlayerTab getInstance() {
        return instance != null ? instance : new DefaultPlayerTab();
    }

    private DefaultPlayerTab(){
        instance = this;
    }

    @Override
    public IChatComponent getFooter() {
        return null;
    }

    @Override
    public IChatComponent getHeader() {
        return null;
    }

    @Override
    public List<TabCell> getCellList() {

        Minecraft minecraft = Minecraft.getMinecraft();

        List<TabCell> tabCells = new ArrayList<>();

        NetHandlerPlayClientWrapper handlerWrapper = NetHandlerPlayClientWrapper.of(minecraft.thePlayer.sendQueue);

        for(GuiPlayerInfo guiPlayerInfo : handlerWrapper.getOrderedServerPlayers()){
            tabCells.add(new TabCell(
                    new ChatComponentText(getFormattedPlayerName(guiPlayerInfo.name, minecraft)),
                    guiPlayerInfo.name,
                    guiPlayerInfo.name,
                    true,
                    guiPlayerInfo.responseTime
            ));
        }

        return tabCells;
    }

    @Override
    public boolean isDrawPlayerHeads() {
        return true;
    }
}
