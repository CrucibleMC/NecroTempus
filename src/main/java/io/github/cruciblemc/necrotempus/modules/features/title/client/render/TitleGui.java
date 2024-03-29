package io.github.cruciblemc.necrotempus.modules.features.title.client.render;

import io.github.cruciblemc.necrotempus.api.title.TitleElement;
import io.github.cruciblemc.necrotempus.api.title.TitleType;
import io.github.cruciblemc.necrotempus.modules.features.title.client.ClientTitleManager;
import io.github.cruciblemc.necrotempus.modules.features.title.component.TimedTitle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class TitleGui extends Gui {

    private final Minecraft minecraft;

    private static TitleGui instance;

    public static TitleGui getInstance() {
        return (instance != null) ? instance : new TitleGui();
    }

    private TitleGui() {
        instance = this;
        this.minecraft = Minecraft.getMinecraft();
    }

    public boolean shouldRender() {
        return ClientTitleManager.getCurrentTitle() != null && finalTime(ClientTitleManager.getCurrentTitle()) >= System.currentTimeMillis();
    }

    public static int maxTime(TimedTitle title) {
        return title.getFadeIn() + title.getStay() + title.getFadeOut();
    }

    public static long finalTime(TimedTitle title) {
        return title.getStartTime() + maxTime(title);
    }

    public void render(ScaledResolution resolution) {

        minecraft.mcProfiler.startSection("necroTempusTitle");
        TimedTitle title = ClientTitleManager.getCurrentTitle();

        long elapsedTime = System.currentTimeMillis() - title.getStartTime();

        int currentState = getCurrentState(title, elapsedTime);

        float currentOpacity = getCurrentOpacity(title, elapsedTime, currentState);

        if (currentOpacity < 8)
            return;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(resolution.getScaledWidth() / 2F, resolution.getScaledHeight() / 2F, 0F);

        int color = new Color(255, 255, 255, (int) currentOpacity).getRGB();

        if (title.hasElement(TitleType.TITLE)) {
            renderTitle(title, color);
        }

        if (title.hasElement(TitleType.SUBTITLE)) {
            renderSubtitle(title, color);
        }

        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        minecraft.mcProfiler.endSection();
    }

    private void renderSubtitle(TimedTitle title, int color) {
        minecraft.mcProfiler.startSection("subtitle");
        TitleElement subtitleElement = title.getElement(TitleType.SUBTITLE);

        GL11.glPushMatrix();
        GL11.glScalef(2F, 2F, 2F);

        int width = -(minecraft.fontRenderer.getStringWidth(subtitleElement.getText().getFormattedText()) / 2);
        int height = 5;

        minecraft.fontRenderer.drawStringWithShadow(subtitleElement.getText().getFormattedText(), width, height, color);
        GL11.glPopMatrix();
        minecraft.mcProfiler.endSection();
    }

    private void renderTitle(TimedTitle title, int color) {
        minecraft.mcProfiler.startSection("title");
        TitleElement titleElement = title.getElement(TitleType.TITLE);

        GL11.glPushMatrix();
        GL11.glScalef(4F, 4F, 4F);

        int width = -(minecraft.fontRenderer.getStringWidth(titleElement.getText().getFormattedText()) / 2);
        int height = -10;

        minecraft.fontRenderer.drawStringWithShadow(titleElement.getText().getFormattedText(), width, height, color);
        GL11.glPopMatrix();
        minecraft.mcProfiler.endSection();
    }

    private static float getCurrentOpacity(TimedTitle title, long elapsedTime, int currentState) {

        return switch (currentState) {
            case 0 -> clamp(((float) elapsedTime / title.getFadeIn()) * 255);
            case 2 ->
                    255 - clamp(((float) (elapsedTime - (title.getFadeIn() + title.getStay())) / title.getFadeOut()) * 255);
            default -> 255;
        };

    }

    private static int getCurrentState(TimedTitle title, long elapsedTime) {
        return (elapsedTime <= title.getFadeIn()) ? 0 :
                (elapsedTime <= (title.getFadeIn() + title.getStay())) ? 1 : 2;
    }

    private static float clamp(float value) {
        return Math.max((float) 0, Math.min((float) 255, value));
    }

}
