package io.github.cruciblemc.necrotempus.modules.features.bossbar.client.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.cruciblemc.necrotempus.Tags;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBar;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarComponent;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarType;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.ClientBossBarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;

@SuppressWarnings("FieldCanBeLocal")
public class BossBarGui extends Gui {

    private static final ResourceLocation BARS_TEXTURES = new ResourceLocation(Tags.MODID, "textures/gui/bars.png");

    private final int BAR_SIZE = 182;
    private final int BAR_SEGMENT_HEIGHT = 5;
    private final int DECORATION_GAP = 20;
    private final int BAR_MARGIN = 20;

    private final Minecraft minecraft;

    private static BossBarGui instance;

    public static BossBarGui getInstance() {
        return (instance != null) ? instance : new BossBarGui();
    }

    private BossBarGui() {
        instance = this;
        this.minecraft = Minecraft.getMinecraft();
    }

    private void render(ScaledResolution scaledResolution) {
        TextureManager textureManager = minecraft.getTextureManager();

        if (!ClientBossBarManager.isEmpty()) {
            int width = scaledResolution.getScaledWidth();
            int y = 12;

            Iterator<BossBar> iterator = ClientBossBarManager.iterator();

            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            while (iterator.hasNext()) {
                BossBar bar = iterator.next();

                if (bar.getCreationTime() != 0 && (bar.getCreationTime() + 2000) < System.currentTimeMillis()) {
                    iterator.remove();
                    continue;
                }

                if (bar.isVisible()) {
                    int x = (width / 2) - BAR_SIZE / 2;
                    drawBossBar(textureManager, x, y, bar);

                    String t = bar.getText().getFormattedText();
                    int textWidth = minecraft.fontRenderer.getStringWidth(t);
                    int textX = (width / 2) - (textWidth / 2);
                    int textY = y - 9;

                    minecraft.fontRenderer.drawStringWithShadow(t, textX, textY, 16777215);
                    y += BAR_MARGIN;

                    if (y >= scaledResolution.getScaledHeight() / 3) {
                        break;
                    }
                }
            }

            GL11.glPopAttrib();
        }
    }

    private void drawBossBar(TextureManager textureManager, int x, int y, BossBarComponent bar) {
        minecraft.mcProfiler.startSection("necroTimeBossBar");
        GL11.glPushMatrix();

        int color = bar.getLazyColor() != -1 ? bar.getLazyColor() : bar.getColor().intValue();
        float RED = ((color >> 16) & 0xFF) / 255F;
        float GREEN = ((color >> 8) & 0xFF) / 255F;
        float BLUE = ((color) & 0xFF) / 255F;

        GL11.glColor4f(RED, GREEN, BLUE, 1F);
        textureManager.bindTexture(BARS_TEXTURES);

        drawTexturedModalRect(x, y, 0, 0, BAR_SIZE, BAR_SEGMENT_HEIGHT);

        if (bar.getType() != BossBarType.FLAT) {
            drawTexturedModalRect(x, y, 0, DECORATION_GAP + (bar.getType().ordinal() - 1) * BAR_SEGMENT_HEIGHT * 2, BAR_SIZE, BAR_SEGMENT_HEIGHT);
        }

        int percentage = (int) (bar.getPercentage() * BAR_SIZE);

        if (percentage > 0) {
            drawTexturedModalRect(x, y, 0, BAR_SEGMENT_HEIGHT, percentage, BAR_SEGMENT_HEIGHT);

            if (bar.getType() != BossBarType.FLAT) {
                drawTexturedModalRect(x, y, 0, (DECORATION_GAP + (bar.getType().ordinal() - 1) * BAR_SEGMENT_HEIGHT * 2 + BAR_SEGMENT_HEIGHT), percentage, BAR_SEGMENT_HEIGHT);
            }
        }

        GL11.glColor4f(1F, 1F, 1F, 1F);
        GL11.glPopMatrix();
        minecraft.mcProfiler.endSection();
    }

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.EXPERIENCE)
            instance.render(event.resolution);
    }
}
