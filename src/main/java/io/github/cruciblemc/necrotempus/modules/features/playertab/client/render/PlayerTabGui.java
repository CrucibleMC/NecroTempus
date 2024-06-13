package io.github.cruciblemc.necrotempus.modules.features.playertab.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.realmsclient.gui.ChatFormatting;
import cpw.mods.fml.common.Loader;
import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.api.playertab.PlayerTab;
import io.github.cruciblemc.necrotempus.api.playertab.TabCell;
import io.github.cruciblemc.necrotempus.modules.features.playertab.client.ClientPlayerTabManager;
import io.github.cruciblemc.necrotempus.modules.features.playertab.client.DefaultPlayerTab;
import io.github.cruciblemc.necrotempus.utils.SkinProvider;
import lain.mods.skinport.init.forge.asm.Hooks;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static net.minecraft.client.entity.AbstractClientPlayer.locationStevePng;
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
        return (
                minecraft.gameSettings.keyBindPlayerList.getIsKeyPressed() &&
                        (!minecraft.isIntegratedServerRunning() || worldScoreboardObjective != null)
        );
    }

    public void render(int width) {

        PlayerTab playerTab = ClientPlayerTabManager.getPlayerTab();
        boolean def = false;

        if (playerTab == null) {
            playerTab = DefaultPlayerTab.getInstance();
            def = true;
        }

        header = playerTab.getHeader();
        footer = playerTab.getFooter();

        drawPlayerHeads = playerTab.isDrawPlayerHeads();

        if (drawPlayerHeads && !NecroTempusConfig.drawPlayersHeads)
            drawPlayerHeads = false;

        if (!def && playerTab.getCellList().isEmpty()) {
            playerTab = DefaultPlayerTab.getInstance();
        }

        drawPlayerList(width, playerTab.getCellList());
    }

    public void drawPlayerList(int width, List<TabCell> cells) {
        minecraft.mcProfiler.startSection("necroTempusPlayerTab");

        if (cells.size() > 80)
            cells = cells.subList(0, 80);

        Scoreboard worldScoreboard = minecraft.theWorld.getScoreboard();
        worldScoreboardObjective = worldScoreboard.func_96539_a(0);

        int[] maxWidths = calculateMaxWidths(cells, worldScoreboard, worldScoreboardObjective);

        int maxTextWidth = maxWidths[0];
        int maxScoreboardScoreWidth = maxWidths[1];

        int cellsCount = cells.size();
        int lastColumnCellCount = cellsCount;
        int columnCount = 1;

        while (lastColumnCellCount > 20)
            lastColumnCellCount = (cellsCount + ++columnCount - 1) / columnCount;

        int maxCellSize = Math.min(columnCount * ((drawPlayerHeads ? 9 : 0) + maxTextWidth + maxScoreboardScoreWidth + 14), width - 50) / columnCount;
        int maxContainerWidth = maxCellSize * columnCount + ((columnCount - 1) * 5);
        int startCellXDrawPosition = width / 2 - maxContainerWidth / 2;

        int currentYDrawPosition = 10;

        List<String> headerList = new ArrayList<>();
        List<String> footerList = new ArrayList<>();

        maxContainerWidth = loadExtraTextElements(maxContainerWidth, width, header, headerList);
        maxContainerWidth = loadExtraTextElements(maxContainerWidth, width, footer, footerList);

        minecraft.mcProfiler.startSection("drawHeader");
        currentYDrawPosition = drawHeaderElement(width, maxContainerWidth, currentYDrawPosition, headerList);
        minecraft.mcProfiler.endSection();

        minecraft.mcProfiler.startSection("drawBackground");
        drawBackground(width, lastColumnCellCount, maxContainerWidth, currentYDrawPosition);
        minecraft.mcProfiler.endSection();

        minecraft.mcProfiler.startSection("drawTabCells");
        drawTabCells(cells, maxTextWidth, maxScoreboardScoreWidth, cellsCount, lastColumnCellCount, maxCellSize, startCellXDrawPosition, currentYDrawPosition);
        minecraft.mcProfiler.endSection();

        minecraft.mcProfiler.startSection("drawFooter");
        //noinspection ReassignedVariable
        currentYDrawPosition = drawFooterElement(width, lastColumnCellCount, maxContainerWidth, currentYDrawPosition, footerList);
        minecraft.mcProfiler.endSection();

        minecraft.mcProfiler.endSection();
    }

    public int[] calculateMaxWidths(List<TabCell> cells, Scoreboard scoreboard, ScoreObjective scoreObjective) {

        int maxTextWidth = 0;
        int maxScoreboardScoreWidth = 0;

        for (TabCell cell : cells) {

            maxTextWidth = Math.max(
                    minecraft.fontRenderer.getStringWidth(cell.getDisplayName().getFormattedText()) + (NecroTempusConfig.drawNumberedPing ? 36 : (NecroTempusConfig.extraPaddingBars ? 36 : 0)),
                    maxTextWidth
            );

            if (scoreObjective != null)
                if (scoreObjective.getCriteria() != health) {
                    if (cell.getDisplayName() != null && !cell.getLinkedUserName().isEmpty()) {
                        Score score = scoreboard.func_96529_a(cell.getLinkedUserName(), scoreObjective);
                        maxScoreboardScoreWidth = Math.max(
                                minecraft.fontRenderer.getStringWidth(" " + score.getScorePoints()),
                                maxScoreboardScoreWidth
                        );
                    }
                } else {
                    maxScoreboardScoreWidth = 90;
                }
        }

        return new int[]{maxTextWidth, maxScoreboardScoreWidth};
    }

    public int loadExtraTextElements(int currentMaxSize, int width, IChatComponent component, List<String> target) {

        int maxSize = currentMaxSize;

        if (component != null) {

            target.addAll(minecraft.fontRenderer.listFormattedStringToWidth(component.getFormattedText(), width - 50));

            for (String text : target)
                maxSize = Math.max(maxSize, minecraft.fontRenderer.getStringWidth(text));
        }

        return maxSize;
    }

    private int drawExtraElement(int width, int maxContainerWidth, int currentYDrawPosition, List<String> elements, int minX, int minY) {

        int maxX = ((width / 2) + (maxContainerWidth / 2) + 1);
        int maxY = (currentYDrawPosition + (elements.size() * minecraft.fontRenderer.FONT_HEIGHT));

        GL11.glPushMatrix();

        drawRect(minX, minY, maxX, maxY, Integer.MIN_VALUE);

        GL11.glPopMatrix();

        for (String line : elements) {

            int lineWidth = minecraft.fontRenderer.getStringWidth(line);
            int x = ((width / 2) - (lineWidth / 2));

            minecraft.fontRenderer.drawStringWithShadow(line, x, currentYDrawPosition, -1);

            currentYDrawPosition += minecraft.fontRenderer.FONT_HEIGHT;
        }

        return currentYDrawPosition;
    }

    private int drawHeaderElement(int width, int maxContainerWidth, int currentYDrawPosition, List<String> headerList) {
        if (!headerList.isEmpty()) {

            int minX = ((width / 2) - (maxContainerWidth / 2) - 1);
            int minY = (currentYDrawPosition - 1);
            currentYDrawPosition = drawExtraElement(width, maxContainerWidth, currentYDrawPosition, headerList, minX, minY);

            ++currentYDrawPosition;

        }
        return currentYDrawPosition;
    }

    private int drawFooterElement(int width, int lastColumnCellCount, int maxContainerWidth, int currentYDrawPosition, List<String> footerList) {

        if (!footerList.isEmpty()) {

            int minX = ((width / 2) - (maxContainerWidth / 2) - 1);
            int minY = ((currentYDrawPosition += (lastColumnCellCount * 9) + 1) - 1);
            drawExtraElement(width, maxContainerWidth, currentYDrawPosition, footerList, minX, minY);

        }

        return currentYDrawPosition;
    }

    private void drawTabCells(List<TabCell> cells, int maxTextWidth, int maxScoreboardScoreWidth, int cellsCount, int lastColumnCellCount, int maxCellSize, int startCellXDrawPosition, int currentYDrawPosition) {

        for (int currentCell = 0; currentCell < cellsCount; ++currentCell) {

            minecraft.mcProfiler.startSection("cell$" + currentCell);
            int cellCount = currentCell / lastColumnCellCount;
            int leftCellCount = currentCell % lastColumnCellCount;

            int minX = startCellXDrawPosition + (cellCount * maxCellSize) + (cellCount * 5);
            int minY = currentYDrawPosition + (leftCellCount * 9);

            GL11.glPushMatrix();

            minecraft.mcProfiler.startSection("cellBackground");
            drawRect(minX, minY, minX + maxCellSize, minY + 8, 553648127);
            minecraft.mcProfiler.endSection();

            GL11.glPopMatrix();

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            if (cellCount >= cells.size()) continue;

            TabCell cell = enforceDisplayName(
                    cells.get(currentCell)
            );

            minecraft.mcProfiler.startSection("drawPlayerHead");
            if (drawPlayerHeads)
                minX = drawPlayerHead(minX, minY, cell);
            minecraft.mcProfiler.endSection();

            minecraft.mcProfiler.startSection("playerName");
            minecraft.fontRenderer.drawStringWithShadow(cell.getDisplayName().getFormattedText(), minX, minY, -1);
            minecraft.mcProfiler.endSection();

            minecraft.mcProfiler.startSection("drawScoreboardValues");
            int textEndX, scoreboardEndX;
            if (cell.isDisplayScore() && (scoreboardEndX = (textEndX = minX + maxTextWidth + 1) + maxScoreboardScoreWidth) - textEndX > 5)
                drawScoreboardValues(worldScoreboardObjective, minY, scoreboardEndX, cell);
            minecraft.mcProfiler.endSection();

            minecraft.mcProfiler.startSection("drawPing");
            drawPing(maxCellSize, minX - (drawPlayerHeads ? 9 : 0), minY, cell);
            minecraft.mcProfiler.endSection();
            minecraft.mcProfiler.endSection();
        }

    }

    private TabCell enforceDisplayName(TabCell cell) {
        if (cell.getDisplayName().getUnformattedText().isEmpty()) {
            if (cell.getLinkedUserName() != null)
                cell.setDisplayName(new ChatComponentText(getFormattedPlayerName(cell.getLinkedUserName(), minecraft)));
        }

        return cell;
    }

    private int drawPlayerHead(int minX, int minY, TabCell cell) {
        ResourceLocation texture = getPlayerSkin(cell.getSkullProfile());

//        float height = 32F;
//
//        try{
//            IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(texture);
//            height = ImageIO.read(resource.getInputStream()).getHeight();
//        } catch (IOException ignored) {}

        minecraft.getTextureManager().bindTexture(texture);
        GL11.glPushMatrix();

        func_152125_a(minX, minY, 8F, 8F, 8, 8, 8, 8, 64.0F, 32F);

        GL11.glPopMatrix();

        minX += 9;
        return minX;
    }

    private void drawBackground(int width, int lastColumnCellCount, int maxContainerWidth, int currentYDrawPosition) {
        int minX = ((width / 2) - (maxContainerWidth / 2) - 1);
        int minY = (currentYDrawPosition - 1);
        int maxX = ((width / 2) + (maxContainerWidth / 2) + 1);
        int maxY = (currentYDrawPosition + (lastColumnCellCount * 9));

        GL11.glPushMatrix();

        drawRect(minX, minY, maxX, maxY, Integer.MIN_VALUE);

        GL11.glPopMatrix();
    }

    private void drawPing(int maxCellSize, int minX, int minY, TabCell tabCell) {

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        int pingStatusIcon = tabCell.getPlayerPing() < 0 ? 5 : (
                tabCell.getPlayerPing() < 150 ? 0 :
                        (tabCell.getPlayerPing() < 300 ? 1 :
                                (tabCell.getPlayerPing() < 600 ? 2 :
                                        (tabCell.getPlayerPing() < 1000 ? 3 : 4)
                                )
                        )
        );


        if (!NecroTempusConfig.drawNumberedPing) {
            minecraft.getTextureManager().bindTexture(icons);

            zLevel += 100.0F;
            GL11.glPushMatrix();
            drawTexturedModalRect(minX + maxCellSize - 11, minY, 0, 176 + (pingStatusIcon * 8), 10, 8);
            GL11.glPopMatrix();
            zLevel -= 100.0F;
            return;
        }

        int[] color = new int[]{-16711936, -256, -14336, -65536, -8355712, -1};
        String ping = tabCell.getPlayerPing() + "ms";
        int size = minecraft.fontRenderer.getStringWidth(ping);
        minecraft.fontRenderer.drawStringWithShadow(ping, minX + maxCellSize - size, minY, color[pingStatusIcon]);

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
        } else {
            score = ChatFormatting.YELLOW + String.valueOf(scorePoints);
        }

        minecraft.fontRenderer.drawStringWithShadow(score, (scoreboardEndX - minecraft.fontRenderer.getStringWidth(score)), minY, 16777215);
    }

    private static final HashSet<String> DOWNLOADING_SKINS = new HashSet<>();
    private static SkinProvider skinProvider;
    private static Constructor<MinecraftProfileTexture> constructor = null;

    @SneakyThrows
    @SuppressWarnings("rawtypes")
    public ResourceLocation getPlayerSkin(GameProfile gameProfile) {

        ResourceLocation resourcelocation = locationStevePng;

        if (gameProfile != null) {

            if (NecroTempusConfig.enableSkinPortCompat && Loader.isModLoaded("skinport") && skinProvider == null) {
                skinProvider = (profile -> Hooks.GuiPlayerTabOverlay_bindTexture(profile, locationStevePng));
            }

            if (skinProvider != null)
                return skinProvider.getSkin(gameProfile);

            if (NecroTempusConfig.enableHeadsFallback && NecroTempusConfig.headsFallbackURL != null && !NecroTempusConfig.headsFallbackURL.isEmpty()) {

                String url = NecroTempusConfig.headsFallbackURL.replaceAll("%name%", gameProfile.getName());

                if (gameProfile.getId() != null) {
                    url = url.replaceAll("%uuid%", gameProfile.getId().toString()).replaceAll("%uuidTrim%", gameProfile.getId().toString().replaceAll("-", ""));
                }

                if (DOWNLOADING_SKINS.contains(url))
                    return locationStevePng;


                if(constructor == null){
                    try{
                        constructor = MinecraftProfileTexture.class.getConstructor(String.class);
                    }catch (Exception ignored){
                        try{
                            constructor = MinecraftProfileTexture.class.getConstructor(String.class, Map.class);
                        }catch (Exception ignored2){}
                    }
                }

                MinecraftProfileTexture skin = null;


                if(constructor != null){
                    if(constructor.getParameterCount() == 1){
                        skin = constructor.newInstance(url);
                    }else{
                        skin = constructor.newInstance(url, (Map) null);
                    }
                }

                if(skin != null){
                    DOWNLOADING_SKINS.add(url);

                    String finalUrl = url;
                    return minecraft.func_152342_ad().func_152789_a(skin, MinecraftProfileTexture.Type.SKIN, (skinPart, skinLoc) -> DOWNLOADING_SKINS.remove(finalUrl));
                }
            }

            try {

                Map profile = minecraft.func_152342_ad().func_152788_a(gameProfile);
                MinecraftProfileTexture skin = (profile != null) ? (MinecraftProfileTexture) profile.getOrDefault(MinecraftProfileTexture.Type.SKIN, null) : null;

                resourcelocation = minecraft.func_152342_ad().func_152792_a(skin, MinecraftProfileTexture.Type.SKIN);

            } catch (Exception ignored) {
            }

        }

        return resourcelocation;
    }

    public static String getFormattedPlayerName(String name, Minecraft minecraft) {

        Scoreboard scoreboard = minecraft.theWorld.getScoreboard();
        Team team = scoreboard.getPlayersTeam(name);

        if (team != null)
            name = team.formatString(name);

        return name;

    }

}
