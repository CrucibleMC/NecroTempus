package io.github.cruciblemc.necrotempus.modules.mixin.mixins.minecraft;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Iterator;

@Mixin(value = GuiIngame.class)
public class GuiIngameMixin extends Gui {

    @Inject(method = "Lnet/minecraft/client/gui/GuiIngame;func_96136_a(Lnet/minecraft/scoreboard/ScoreObjective;IILnet/minecraft/client/gui/FontRenderer;)V", at = @At("HEAD"), cancellable = true)
    protected void func_96136_a(ScoreObjective scoreObjective, int y, int x, FontRenderer fontRenderer, CallbackInfo callbackInfo) {

        Scoreboard scoreboard = scoreObjective.getScoreboard();
        Collection<?> scores = scoreboard.func_96534_i(scoreObjective);

        if (scores.size() > 15)
            scores = Lists.newArrayList(Iterables.skip(scores, scores.size() - 15));

        int maxWidth = fontRenderer.getStringWidth(scoreObjective.getDisplayName());
        String s;

        for (Iterator<?> iterator = scores.iterator(); iterator.hasNext(); maxWidth = Math.max(maxWidth, fontRenderer.getStringWidth(s))) {
            Score score = (Score) iterator.next();
            ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
            s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName()) + ": " + (NecroTempusConfig.hideScores ? "" : EnumChatFormatting.RED + String.valueOf(score.getScorePoints()));
        }

        if (NecroTempusConfig.hideScores)
            maxWidth -= 2;

        int height = scores.size() * fontRenderer.FONT_HEIGHT;

        int padding = 3;

        int drawX = x - maxWidth - padding;
        int drawY = (y / 2) + (height / 3);

        Iterator<Score> scoreIterator = (Iterator<Score>) scores.iterator();

        int currentIndex = 0;

        while (scoreIterator.hasNext()) {

            ++currentIndex;

            Score score = scoreIterator.next();

            ScorePlayerTeam playerTeam = scoreboard.getPlayersTeam(score.getPlayerName());
            String playerName = ScorePlayerTeam.formatPlayerName(playerTeam, score.getPlayerName());
            String scoreValue = EnumChatFormatting.RED + String.valueOf(score.getScorePoints());

            int startY = drawY - (currentIndex * fontRenderer.FONT_HEIGHT);
            int endX = x - (padding + 2);

            GL11.glPushMatrix();

            drawRect(drawX - 2, startY, endX, startY + fontRenderer.FONT_HEIGHT, 1342177280);

            GL11.glPopMatrix();

            fontRenderer.drawString(playerName, drawX, startY, -1);

            if (!NecroTempusConfig.hideScores)
                fontRenderer.drawString(scoreValue, endX - fontRenderer.getStringWidth(scoreValue), startY, -1);

            if (!scoreIterator.hasNext()) {

                String title = scoreObjective.getDisplayName();

                GL11.glPushMatrix();

                drawRect(drawX - 2, startY - fontRenderer.FONT_HEIGHT - 1, endX, startY - 1, (NecroTempusConfig.titleBackground ? 1610612736 : 1342177280));
                drawRect(drawX - 2, startY - 1, endX, startY, 1342177280);

                GL11.glPopMatrix();

                fontRenderer.drawString(title, drawX + maxWidth / 2 - fontRenderer.getStringWidth(title) / 2, startY - fontRenderer.FONT_HEIGHT, -1);

            }
        }

        callbackInfo.cancel();
    }

}
