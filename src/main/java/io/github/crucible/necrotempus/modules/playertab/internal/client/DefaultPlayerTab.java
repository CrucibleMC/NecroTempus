package io.github.crucible.necrotempus.modules.playertab.internal.client;

import com.mojang.authlib.GameProfile;
import io.github.crucible.necrotempus.modules.playertab.internal.component.PlayerTab;
import io.github.crucible.necrotempus.modules.playertab.internal.component.TabCell;
import io.github.crucible.necrotempus.utils.NetHandlerPlayClientNT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.List;

import static io.github.crucible.necrotempus.modules.playertab.internal.render.PlayerTabGui.getFormattedPlayerName;

public class DefaultPlayerTab extends PlayerTab {

    private static PlayerTab instance;

    public static PlayerTab getInstance() {
        return instance != null ? instance : new DefaultPlayerTab();
    }

    private DefaultPlayerTab(){
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

        Minecraft minecraft = Minecraft.getMinecraft();

        List<TabCell> tabCells = new ArrayList<>();

        NetHandlerPlayClientNT handlerWrapper = NetHandlerPlayClientNT.of(minecraft.thePlayer.sendQueue);

        for(GuiPlayerInfo guiPlayerInfo : handlerWrapper.getOrderedServerPlayers()){

            EntityPlayer entityPlayer = minecraft.theWorld.getPlayerEntityByName(guiPlayerInfo.name);

            GameProfile gameProfile = entityPlayer != null ? entityPlayer.getGameProfile() : new GameProfile(null, guiPlayerInfo.name);

            tabCells.add(new TabCell(
                    new ChatComponentText(getFormattedPlayerName(guiPlayerInfo.name, minecraft)),
                    guiPlayerInfo.name,
                    gameProfile,
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
