package io.github.crucible.necrotempus.modules.playertab.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.realmsclient.gui.ChatFormatting;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.crucible.necrotempus.CrucibleNecroTempus;
import io.github.crucible.necrotempus.modules.bossbar.internal.render.BossBarGui;
import io.github.crucible.necrotempus.modules.playertab.PlayerTabManager;
import io.github.crucible.necrotempus.modules.playertab.component.PlayerTab;
import io.github.crucible.necrotempus.modules.playertab.component.TabCell;
import io.github.crucible.necrotempus.utils.NetHandlerPlayClientWrapper;
import io.github.crucible.necrotempus.utils.ServerUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.minecraft.scoreboard.IScoreObjectiveCriteria.health;

@SuppressWarnings("unchecked")
@Getter
@Setter
public class PlayerTabGui extends Gui {

    private final Minecraft minecraft;

    private boolean drawPlayerHeads = true;

    private IChatComponent footer;
    private IChatComponent header;

    private long lastTimeOpened;
    private boolean isBeingRendered;

    ScoreObjective worldScoreboardObjective;

    private static PlayerTabGui instance;

    public static PlayerTabGui getInstance() {
        return (instance != null) ? instance : new PlayerTabGui();
    }

    public PlayerTabGui() {
        instance = this;
        this.minecraft = Minecraft.getMinecraft();
    }

    public boolean shouldRender() {
        return CrucibleNecroTempus.getInstance().getnecroTempusApi().isCustomServerTabEnabled() &&
                (minecraft.gameSettings.keyBindPlayerList.getIsKeyPressed() &&
                        (!minecraft.isIntegratedServerRunning() ||
                                minecraft.thePlayer.sendQueue.playerInfoList.size() > 1 ||
                                worldScoreboardObjective != null)
                );
    }

    public void render(int width, PlayerTabManager playerTabManager){

        PlayerTab playerTab = playerTabManager.getPlayerTab();

        if(playerTab == null){

            playerTab = new PlayerTab();
            playerTab.setHeader(null);
            playerTab.setFooter(null);
            playerTab.setDrawPlayerHeads(true);

            List<TabCell> tabCells = new ArrayList<>();

            NetHandlerPlayClientWrapper handlerWrapper = NetHandlerPlayClientWrapper.of(minecraft.thePlayer.sendQueue);

            for(GuiPlayerInfo guiPlayerInfo : handlerWrapper.getOrderedServerPlayers()){
                tabCells.add(new TabCell(
                        new ChatComponentText(getFormattedPlayerName(guiPlayerInfo.name)),
                        guiPlayerInfo.name,
                        guiPlayerInfo.name,
                        true,
                        guiPlayerInfo.responseTime
                ));
            }

            playerTab.setCellList(tabCells);

        }

        header = playerTab.getHeader();
        footer = playerTab.getFooter();
        drawPlayerHeads = playerTab.isDrawPlayerHeads();
        drawPlayerList(width, playerTab.getCellList());
    }

    public void drawPlayerList(int width, List<TabCell> cells){

        if(cells.size() > 80)
            cells = cells.subList(0, 80);

        int maxTextWidth = 0;
        int maxScoreboardScoreWidth = 0;

        Scoreboard worldScoreboard = minecraft.theWorld.getScoreboard();
        worldScoreboardObjective = worldScoreboard.func_96539_a(0);

        for(TabCell cell : cells){

            maxTextWidth = Math.max(
                    minecraft.fontRenderer.getStringWidth(cell.getDisplayName().getFormattedText()),
                    maxTextWidth
            );

            if(worldScoreboardObjective != null && worldScoreboardObjective.getCriteria() != health)
                if(cell.getDisplayName() != null && !cell.getLinkedUserName().isEmpty()){
                    Score score = worldScoreboard.func_96529_a(cell.getLinkedUserName(), worldScoreboardObjective);
                    maxScoreboardScoreWidth = Math.max(
                            minecraft.fontRenderer.getStringWidth(" " + score.getScorePoints()),
                            maxScoreboardScoreWidth
                    );
                }
        }

        int cellsCount = cells.size();
        int lastColumnCellCount = cellsCount;
        int columnCount = 1;

        while (lastColumnCellCount > 20){
            lastColumnCellCount = (cellsCount + ++columnCount - 1) / columnCount;
        }

        int finalScoreboardWidth = (worldScoreboardObjective != null && worldScoreboardObjective.getCriteria() == health) ? 90 : maxScoreboardScoreWidth;

        int maxCellSize = Math.min(columnCount * ((drawPlayerHeads ? 9 : 0) + maxTextWidth + finalScoreboardWidth + 13), width - 50) / columnCount;

        int A = width / 2 - (maxCellSize * columnCount + (columnCount - 1) * 5) / 2;

        int currentYDrawPosition = 10;

        int maxContainerWidth = maxCellSize * columnCount + ((columnCount - 1) * 5);

        List<String> headerList = null;
        List<String> footerList = null;

        if (this.header != null) {

            headerList = minecraft.fontRenderer.listFormattedStringToWidth(this.header.getFormattedText(), width - 50);

            for (String header : headerList)
                maxContainerWidth = Math.max(maxContainerWidth, minecraft.fontRenderer.getStringWidth(header));
        }

        if (this.footer != null) {

            footerList = minecraft.fontRenderer.listFormattedStringToWidth(this.footer.getFormattedText(), width - 50);

            for (String footer : footerList)
                maxContainerWidth = Math.max(maxContainerWidth, minecraft.fontRenderer.getStringWidth(footer));

        }

        if (headerList != null) {

            int minX = ((width / 2) - (maxContainerWidth / 2) - 1);
            int minY = (currentYDrawPosition - 1);
            int maxX = ((width / 2) + (maxContainerWidth / 2) + 1);
            int maxY = (currentYDrawPosition + (cellsCount * minecraft.fontRenderer.FONT_HEIGHT));

            GL11.glPushMatrix();

            drawRect(minX, minY, maxX, maxY, Integer.MIN_VALUE);

            GL11.glPopMatrix();

            for (String line : headerList) {

                int lineWidth = minecraft.fontRenderer.getStringWidth(line);
                int x = ((width / 2) - (lineWidth / 2));

                minecraft.fontRenderer.drawStringWithShadow(line, x, currentYDrawPosition, -1);

                currentYDrawPosition += minecraft.fontRenderer.FONT_HEIGHT;
            }

            ++currentYDrawPosition;

        }

        {
            int minX = ((width / 2) - (maxContainerWidth / 2) - 1);
            int minY = (currentYDrawPosition - 1);
            int maxX = ((width / 2) + (maxContainerWidth / 2) + 1);
            int maxY = (currentYDrawPosition + (lastColumnCellCount * 9));

            GL11.glPushMatrix();

            drawRect(minX, minY, maxX, maxY, Integer.MIN_VALUE);

            GL11.glPopMatrix();

        }

        for (int currentCell = 0; currentCell < cellsCount; ++currentCell) {

            int cellCount = currentCell / lastColumnCellCount;
            int leftCellCount = currentCell % lastColumnCellCount;

            int minX = A + (cellCount * maxCellSize) + (cellCount * 5);
            int minY = currentYDrawPosition + (leftCellCount * 9);

            GL11.glPushMatrix();

            drawRect(minX, minY, minX + maxCellSize, minY + 8, 553648127);

            GL11.glPopMatrix();

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            if(cellCount >= cells.size()) continue;

            TabCell cell = cells.get(cellCount);

            if(cell.getDisplayName().getUnformattedText().isEmpty()){
                if(cell.getLinkedUserName() != null)
                    cell.setDisplayName(new ChatComponentText(getFormattedPlayerName(cell.getLinkedUserName())));
            }

            if(drawPlayerHeads){

                minecraft.getTextureManager().bindTexture(getPlayerSkin(new GameProfile(null, cell.getDisplaySkullName())));

                GL11.glPushMatrix();

                func_152125_a(minX, minY, 8F, 8F, 8, 8, 8, 8, 64.0F, 32.0F);

                GL11.glPopMatrix();

                minX += 9;

            }

            minecraft.fontRenderer.drawStringWithShadow(cell.getDisplayName().getFormattedText(), minX, minY, -1);

            int textEndX, scoreboardEndX;

            if (cell.isDisplayScore() && (scoreboardEndX = (textEndX = minX + maxTextWidth + 1) + maxScoreboardScoreWidth) - textEndX > 5) {
                this.drawScoreboardValues(worldScoreboardObjective, minY, scoreboardEndX, cell);
            }

            this.drawPing(maxCellSize, minX - (drawPlayerHeads ? 9 : 0), minY, cell);

        }

        if (footerList != null) {

            int minX = ((width / 2) - (maxContainerWidth / 2) - 1);
            int minY = ((currentYDrawPosition += (lastColumnCellCount * 9) + 1) - 1);
            int maxX = ((width / 2) + (maxContainerWidth / 2) + 1);
            int maxY = (currentYDrawPosition + (cellsCount * minecraft.fontRenderer.FONT_HEIGHT));

            GL11.glPushMatrix();

            drawRect(minX, minY, maxX, maxY, Integer.MIN_VALUE);

            GL11.glPopMatrix();

            for (String line : footerList) {

                int lineWidth = minecraft.fontRenderer.getStringWidth(line);
                int x = ((width / 2) - (lineWidth / 2));

                minecraft.fontRenderer.drawStringWithShadow(line, x, currentYDrawPosition, -1);

                currentYDrawPosition += minecraft.fontRenderer.FONT_HEIGHT;
            }

        }

    }
    
    private void drawPing(int maxCellSize, int minX, int minY, TabCell tabCell) {

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        minecraft.getTextureManager().bindTexture(icons);

        int pingStatusIcon = tabCell.getPlayerPing() < 0 ? 5 : (
                tabCell.getPlayerPing() < 150 ? 0 :
                        (tabCell.getPlayerPing() < 300 ? 1 :
                                (tabCell.getPlayerPing() < 600 ? 2 :
                                        (tabCell.getPlayerPing() < 1000 ? 3 : 4)
                                )
                        )
                );


        zLevel += 100.0F;
        GL11.glPushMatrix();
        drawTexturedModalRect(minX + maxCellSize - 11, minY, 0, 176 + (pingStatusIcon * 8), 10, 8);
        GL11.glPopMatrix();
        zLevel -= 100.0F;
    }

    private void drawScoreboardValues(ScoreObjective scoreObjective, int minY, int scoreboardEndX, TabCell tabCell) {

        int scorePoints = scoreObjective.getScoreboard().func_96529_a(
                tabCell.getLinkedUserName(),
                scoreObjective
        ).getScorePoints();

        String score;
        if (scoreObjective.getCriteria() == health) {
            //noinspection UnnecessaryUnicodeEscape
            score = ChatFormatting.RED + "\u2764 " + scorePoints;
        }else {
            score = ChatFormatting.YELLOW + String.valueOf(scorePoints);
        }

        minecraft.fontRenderer.drawStringWithShadow(score, (scoreboardEndX - minecraft.fontRenderer.getStringWidth(score)), minY, 16777215);
    }

    @SuppressWarnings("rawtypes")
    public ResourceLocation getPlayerSkin(GameProfile gameProfile){
        ResourceLocation resourcelocation = AbstractClientPlayer.locationStevePng;

        if (gameProfile != null) {

            Map profile =  minecraft.func_152342_ad().func_152788_a(gameProfile);
            MinecraftProfileTexture minecraftProfileTexture = (profile != null) ? (MinecraftProfileTexture) profile.getOrDefault(MinecraftProfileTexture.Type.SKIN, null) : null;

            if(minecraftProfileTexture != null)
                resourcelocation = minecraft.func_152342_ad().func_152792_a(minecraftProfileTexture, MinecraftProfileTexture.Type.SKIN);
        }

        return resourcelocation;
    }

    public String getFormattedPlayerName(String name) {

        Scoreboard scoreboard = minecraft.theWorld.getScoreboard();
        Team team = scoreboard.getPlayersTeam(name);

        if(team != null)
            name = team.formatString(name);

        return name;

    }

}
