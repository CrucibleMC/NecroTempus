package io.github.crucible.timemachine.bossbar.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import io.github.crucible.timemachine.bossbar.BossBarAPI;
import io.github.crucible.timemachine.bossbar.BossBarColor;
import io.github.crucible.timemachine.bossbar.BossBarComponent;
import io.github.crucible.timemachine.bossbar.BossBarType;
import io.github.crucible.timemachine.bossbar.server.BossBar;
import io.github.crucible.timemachine.bossbar.server.BossBarTemp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BossBarGui extends Gui {

    private final ResourceLocation textures = new ResourceLocation("crucibletimemachine","textures/gui/bars.png");

    private final Minecraft minecraft;
    private TextureManager textureManager;

    //private static final LinkedHashMap<UUID, Object> barSet = Maps.newLinkedHashMap();
    private static final ConcurrentLinkedQueue<Map.Entry<UUID, Object>> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();

    private static BossBarGui instance;

    public static BossBarGui getInstance() {
        return (instance != null) ? instance : new BossBarGui(Minecraft.getMinecraft());
    }

    public BossBarGui(Minecraft minecraft){
        instance = this;

        this.minecraft = minecraft;
    }

    public static void addBar(BossBar bossBar){
        //barSet.put(bossBar.getUuid(), bossBar);


        for(Map.Entry<?,?> e : concurrentLinkedQueue){
            if(e.getKey().equals(bossBar.getUuid())){
                concurrentLinkedQueue.remove(e);
                break;
            }
        }

        Map.Entry<UUID, Object> entry = data(bossBar.getUuid(), bossBar);

        concurrentLinkedQueue.offer(entry);
    }

    public static void removeBar(BossBar bossBar){
        //barSet.remove(bossBar.getUuid());
        concurrentLinkedQueue.remove(data(bossBar.getUuid(), bossBar));
    }

    private void render(){
        textureManager = minecraft.getTextureManager();

        //if(!barSet.values().isEmpty()){
        if(!concurrentLinkedQueue.isEmpty()){

            int y = minecraft.displayHeight;
            int j = 12;

            Iterator<?> iterator = concurrentLinkedQueue.iterator(); //barSet.values().iterator();

            while(iterator.hasNext()){
                Object o = ((Map.Entry<?, ?>)iterator.next()).getValue();
                BossBar bossBar_ = (BossBar) o;

                if(o instanceof BossBarTemp){
                    if(System.currentTimeMillis() - ((BossBarTemp) o).getStartTime() >= 2000){
                        iterator.remove();
                    }
                }

                if(bossBar_.isVisible()){

                    int k = y / 2 - 91;

                    GL11.glPushMatrix();
                    GL11.glPushMatrix();

                    GL11.glColor4f(1F,1F,1F,1F);
                    render(k, j, bossBar_);

                    String t = bossBar_.getText().getFormattedText();

                    int l = minecraft.fontRenderer.getStringWidth(t);
                    int a = y / 2 - l / 2;
                    int b = j - 9;

                    minecraft.fontRenderer.drawStringWithShadow(t, a, b,16777215);

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
        drawTexturedModalRect(x,y,0,bar.getColor().ordinal() * 5 * 2,182,5);

        if(bar.getType() != BossBarType.FLAT){
            drawTexturedModalRect(x,y,0,80 + (bar.getType().ordinal() - 1) * 5 * 2 + 5,182,5);
        }

        int a = (int) ((bar.getPercent() * 0.01) * 183.0F);

        if(a > 0){
            drawTexturedModalRect(x,y,0,bar.getColor().ordinal() * 5 * 2 + 5, a,5);

            if(bar.getType() != BossBarType.FLAT){
                drawTexturedModalRect(x,y,0,80 + (bar.getType().ordinal() - 1) * 5 * 2 + 5, a,5);
            }

        }

    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Post event) {

        if(event.entity instanceof IBossDisplayData){

            IBossDisplayData bossDisplayData = (IBossDisplayData) event.entity;
            BossBarTemp bossBarTemp;

            if(event.entity instanceof EntityDragon){
                bossBarTemp = BossBarAPI.createTemp(event.entity.getUniqueID(), (ChatComponentText) bossDisplayData.func_145748_c_(), (bossDisplayData.getHealth() * 100) / bossDisplayData.getMaxHealth(), BossBarColor.PURPLE, BossBarType.NOTCHED_20);
            }else if(event.entity instanceof EntityWither){
                bossBarTemp = BossBarAPI.createTemp(event.entity.getUniqueID(), (ChatComponentText) bossDisplayData.func_145748_c_(), (bossDisplayData.getHealth() * 100) / bossDisplayData.getMaxHealth(), BossBarColor.RED, BossBarType.NOTCHED_20);
            }else {
                bossBarTemp = BossBarAPI.createTemp(event.entity.getUniqueID(), (ChatComponentText) bossDisplayData.func_145748_c_(), (bossDisplayData.getHealth() * 100) / bossDisplayData.getMaxHealth(), BossBarColor.PINK, BossBarType.FLAT);
            }

            addBar(bossBarTemp);
        }

    }

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.EXPERIENCE) return;
        instance.render();
    }

    private static Map.Entry<UUID, Object> data(UUID uuid, Object object){
        return new Map.Entry<UUID, Object>() {
            @Override
            public UUID getKey() {
                return uuid;
            }

            @Override
            public Object getValue() {
                return object;
            }

            @Override
            public Object setValue(Object value) {
                return null;
            }
        };
    }

}
