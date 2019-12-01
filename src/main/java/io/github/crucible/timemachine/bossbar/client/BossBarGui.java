package io.github.crucible.timemachine.bossbar.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.crucible.timemachine.bossbar.BossBarComponent;
import io.github.crucible.timemachine.bossbar.BossBarType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;


import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarGui extends Gui {

    private ResourceLocation textures = new ResourceLocation("crucibletimemachine","/textures/gui/bars.png".substring(1));
    private Minecraft minecraft;
    private TextureManager textureManager;
    private static final ConcurrentHashMap<UUID,BossBarComponent> barSet = new ConcurrentHashMap<>();
    private static BossBarGui instance = new BossBarGui(Minecraft.getMinecraft());

    public static BossBarGui getInstance() {
        return instance;
    }

    public static void addBar(BossBarComponent bossBar){
        if(barSet.containsKey(bossBar.getUuid())){
            barSet.replace(bossBar.getUuid(), bossBar);
        }
        barSet.put(bossBar.getUuid(),bossBar);
    }

    public static void removeBar(BossBarComponent bossBar){
        barSet.remove(bossBar.getUuid());
    }

    public BossBarGui(Minecraft minecraft){
        this.minecraft = minecraft;
    }

    public void render(){
        textureManager = minecraft.getTextureManager();
        if(!barSet.isEmpty()){

            int y = minecraft.displayHeight;
            int j = 12;

            for(BossBarComponent bossBar : barSet.values()){
                if(bossBar.isVisible()){
                    int k = y / 2 - 91;

                    GL11.glPushMatrix();
                    GL11.glPushMatrix();

                    GL11.glColor4f(1F,1F,1F,1F);
                    render(k,j,bossBar);
                    String t = bossBar.getText().getFormattedText();
                    int l = minecraft.fontRenderer.getStringWidth(t);

                    int a = y / 2 - l / 2;
                    int b = j - 9;
                    minecraft.fontRenderer.drawStringWithShadow(t,a,b,16777215);
                    GL11.glPopMatrix();
                    GL11.glPopMatrix();

                    j += 20;

                    if(j >= minecraft.displayHeight/3){
                        break;
                    }
                }
            }
        }
    }

    private void render(int x, int y, BossBarComponent bar){
        textureManager.bindTexture(textures);
        drawTexturedModalRect(x,y,0,bar.getColor().ordinal()*5*2,182,5);

        if(bar.getType() != BossBarType.FLAT){
            drawTexturedModalRect(x,y,0,80 + (bar.getType().ordinal()-1)*5*2 + 5,182,5);
        }

        int a = (int) ((bar.getPercent()*0.01) * 183.0F);
        if(a > 0){
            drawTexturedModalRect(x,y,0,bar.getColor().ordinal() * 5 * 2 + 5, a,5);

            if(bar.getType() != BossBarType.FLAT){
                drawTexturedModalRect(x,y,0,80 + (bar.getType().ordinal() - 1) * 5 * 2 + 5, a,5);
            }

        }

    }

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.EXPERIENCE) return;

        instance.render();
    }

}
