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

    @SuppressWarnings("unchecked")
    @Inject(method = "Lnet/minecraft/client/gui/GuiIngame;func_96136_a(Lnet/minecraft/scoreboard/ScoreObjective;IILnet/minecraft/client/gui/FontRenderer;)V", at = @At("HEAD"), cancellable = true)
    protected void func_96136_a(ScoreObjective scoreObjective, int y, int x, FontRenderer fontRenderer, CallbackInfo callbackInfo) {

        Scoreboard scoreboard = scoreObjective.getScoreboard();
        Collection<?> scores = scoreboard.func_96534_i(scoreObjective);

        if (scores.size() > 15) {
            scores = Lists.newArrayList(Iterables.skip(scores, scores.size() - 15));
        }

        int leftPadding = 1;
        int rightPadding = 1;
        int marginRight = 2;
        int scoreSpacing = 2;
        int titleVerticalPadding = 2;

        int maxNameWidth = fontRenderer.getStringWidth(scoreObjective.getDisplayName());
        int maxScoreWidth = 0;

        for (Score score : (Collection<Score>) scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String formattedName = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            String scoreText = EnumChatFormatting.RED + String.valueOf(score.getScorePoints());

            int nameWidth;
            if (formattedName.contains("||")) {
                String[] parts = formattedName.split("\\|\\|", 2);
                nameWidth = fontRenderer.getStringWidth(parts[0]) + fontRenderer.getStringWidth(parts[1]);
            } else {
                nameWidth = fontRenderer.getStringWidth(formattedName);
            }
            if (nameWidth > maxNameWidth) {
                maxNameWidth = nameWidth;
            }
            if (!NecroTempusConfig.hideScores) {
                int sWidth = fontRenderer.getStringWidth(scoreText);
                if (sWidth > maxScoreWidth) {
                    maxScoreWidth = sWidth;
                }
            }
        }

        int contentWidth = maxNameWidth;
        if (!NecroTempusConfig.hideScores) {
            contentWidth += scoreSpacing + maxScoreWidth;
        }

        int totalWidth = contentWidth + leftPadding + rightPadding;

        int boxRight = x - marginRight;
        int boxLeft = boxRight - totalWidth;

        int innerRight = boxRight - rightPadding;

        int lineHeight = fontRenderer.FONT_HEIGHT;
        int totalHeight = scores.size() * lineHeight;
        int baseY = (y / 2) + (totalHeight / 3);

        Iterator<Score> scoreIterator = (Iterator<Score>) scores.iterator();
        int index = 0;
        int scoresSize = scores.size();
        int lastLineY = 0;

        while (scoreIterator.hasNext()) {
            index++;
            Score score = scoreIterator.next();
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String formattedName = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            String scoreText = EnumChatFormatting.RED + String.valueOf(score.getScorePoints());

            int currentY = baseY - (index * lineHeight);
            lastLineY = currentY;

            GL11.glPushMatrix();
            drawRect(boxLeft - 2, currentY, boxRight, currentY + lineHeight, 1342177280);
            GL11.glPopMatrix();

            int currentScoreWidth = (!NecroTempusConfig.hideScores) ? fontRenderer.getStringWidth(scoreText) : 0;
            int scoreX = innerRight - currentScoreWidth; // score alinhado à direita na área interna

            if (formattedName.contains("||")) {
                String[] parts = formattedName.split("\\|\\|", 2);
                String leftText = parts[0];
                String rightText = parts[1];

                fontRenderer.drawString(leftText, boxLeft, currentY, -1);

                int rightBoundary = (!NecroTempusConfig.hideScores) ? (scoreX - scoreSpacing) : innerRight;
                int rightTextWidth = fontRenderer.getStringWidth(rightText);
                int rightTextX = rightBoundary - rightTextWidth;
                fontRenderer.drawString(rightText, rightTextX, currentY, -1);
            } else {
                fontRenderer.drawString(formattedName, boxLeft, currentY, -1);
            }

            if (!NecroTempusConfig.hideScores) {
                fontRenderer.drawString(scoreText, scoreX, currentY, -1);
            }
        }

        if (lastLineY != 0) {
            String title = scoreObjective.getDisplayName();
            int titleTop = lastLineY - lineHeight + titleVerticalPadding;

            GL11.glPushMatrix();
            drawRect(boxLeft - 2, titleTop - 1, boxRight, titleTop, (NecroTempusConfig.titleBackground ? 1610612736 : 1342177280));
            drawRect(boxLeft - 2, titleTop, boxRight, lastLineY, 1342177280);
            GL11.glPopMatrix();

            int titleWidth = fontRenderer.getStringWidth(title);
            int titleX = boxLeft + ((totalWidth - titleWidth) / 2);
            fontRenderer.drawString(title, titleX, titleTop, -1);
        }

        callbackInfo.cancel();
    }

}
