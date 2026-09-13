package io.github.cruciblemc.necrotempus.modules.features.glow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.modules.features.glow.GlowVolume;
import io.github.cruciblemc.necrotempus.modules.features.glow.GlowingEntityRegistry;

@SideOnly(Side.CLIENT)
public class GlowClientManager {

    private static final GlowClientManager INSTANCE = new GlowClientManager();
    private static final int DEBUG_VOLUME_ID = Integer.MIN_VALUE;

    private final GlowingEntityRegistry registry = new GlowingEntityRegistry();
    private KeyBinding debugKey;
    private KeyBinding debugVolumeKey;
    private boolean debugKeyWasDown = false;
    private boolean debugVolumeKeyWasDown = false;

    private GlowClientManager() {}

    public static GlowClientManager getInstance() {
        return INSTANCE;
    }

    public GlowingEntityRegistry registry() {
        return registry;
    }

    void registerDebugKey() {
        if (NecroTempusConfig.glowDebugKey && debugKey == null) {
            debugKey = new KeyBinding("Debug: Glow looked-at entity", Keyboard.KEY_G, "NecroTempus");
            ClientRegistry.registerKeyBinding(debugKey);
            debugVolumeKey = new KeyBinding("Debug: Glow test volume", Keyboard.KEY_V, "NecroTempus");
            ClientRegistry.registerKeyBinding(debugVolumeKey);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        registry.tick();
        handleDebugKey();
        handleDebugVolumeKey();
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        registry.clear();
    }

    private void handleDebugKey() {
        if (debugKey == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        boolean down = debugKey.getIsKeyPressed();
        if (down && !debugKeyWasDown) {
            Entity e = findDebugTarget(mc);
            if (e != null) {
                registry.set(e.getEntityId(), 0xFFFFFF, 200);
                mc.thePlayer.addChatMessage(new ChatComponentText("Glowing entity " + e.getEntityId()));
            }
        }
        debugKeyWasDown = down;
    }

    private void handleDebugVolumeKey() {
        if (debugVolumeKey == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        boolean down = debugVolumeKey.getIsKeyPressed();
        if (down && !debugVolumeKeyWasDown) {
            int minX = MathHelper.floor_double(mc.thePlayer.posX) >> 4 << 4;
            int minZ = MathHelper.floor_double(mc.thePlayer.posZ) >> 4 << 4;
            registry.setVolume(
                DEBUG_VOLUME_ID,
                new GlowVolume(minX, 0.0D, minZ, minX + 16.0D, mc.theWorld.getHeight(), minZ + 16.0D, 0xFF0000, 200));
            mc.thePlayer.addChatMessage(new ChatComponentText("Glowing test volume for chunk"));
        }
        debugVolumeKeyWasDown = down;
    }

    private Entity findDebugTarget(Minecraft mc) {
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop != null && mop.entityHit != null) return mop.entityHit;

        double reach = mc.playerController.getBlockReachDistance();
        Vec3 start = mc.thePlayer.getPosition(1.0F);
        Vec3 look = mc.thePlayer.getLook(1.0F);
        Vec3 end = start.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        double closest = reach;
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mop.hitVec != null) {
            closest = start.distanceTo(mop.hitVec);
        }

        EntityItem result = null;
        AxisAlignedBB search = mc.thePlayer.boundingBox
            .addCoord(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach)
            .expand(1.0D, 1.0D, 1.0D);
        for (EntityItem item : mc.theWorld.getEntitiesWithinAABB(EntityItem.class, search)) {
            AxisAlignedBB box = item.boundingBox
                .expand(item.getCollisionBorderSize(), item.getCollisionBorderSize(), item.getCollisionBorderSize());
            MovingObjectPosition hit = box.calculateIntercept(start, end);
            if (hit == null) continue;
            double distance = start.distanceTo(hit.hitVec);
            if (distance < closest) {
                closest = distance;
                result = item;
            }
        }
        return result;
    }
}
