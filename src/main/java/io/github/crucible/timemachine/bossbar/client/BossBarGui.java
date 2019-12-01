package io.github.crucible.timemachine.bossbar.client;

import com.google.common.collect.Sets;
import io.github.crucible.timemachine.bossbar.BossBarColor;
import io.github.crucible.timemachine.bossbar.BossBarType;
import io.github.crucible.timemachine.bossbar.server.BossBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class BossBarGui extends Gui {

    private ResourceLocation textures = new ResourceLocation("crucibletimemachine","/textures/gui/bars.png".substring(1));
    private Minecraft minecraft;
    private TextureManager textureManager;
    private final Set<BossBar> barSet = Sets.newHashSet();

    public BossBarGui(Minecraft minecraft){
        this.minecraft = minecraft;
        barSet.add(new BossBar(new ChatComponentText(new Random().nextLong() + ""), BossBarColor.RED,BossBarType.NOTCHED_6,57F,UUID.randomUUID()));
        barSet.add(new BossBar(new ChatComponentText(new Random().nextLong() + ""), BossBarColor.GREEN,BossBarType.NOTCHED_12,33F,UUID.randomUUID()));
        barSet.add(new BossBar(new ChatComponentText(new Random().nextLong() + ""), BossBarColor.PINK,BossBarType.FLAT,78F,UUID.randomUUID()));
        barSet.add(new BossBar(new ChatComponentText(new Random().nextLong() + ""), BossBarColor.BLUE,BossBarType.NOTCHED_10,100F,UUID.randomUUID()));
        barSet.add(new BossBar(new ChatComponentText(new Random().nextLong() + ""), BossBarColor.WHITE,BossBarType.NOTCHED_12,15F,UUID.randomUUID()));
        barSet.add(new BossBar(new ChatComponentText(new Random().nextLong() + ""), BossBarColor.PURPLE,BossBarType.FLAT,85F,UUID.randomUUID()));
    }

    public void render(){
        textureManager = minecraft.getTextureManager();
        if(!barSet.isEmpty()){

            int y = minecraft.displayHeight;
            int j = 12;

            for(BossBar bossBar : barSet){
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

    private void render(int x, int y, BossBar bar){
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

}
