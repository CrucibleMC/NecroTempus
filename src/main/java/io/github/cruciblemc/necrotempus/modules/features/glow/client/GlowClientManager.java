package io.github.cruciblemc.necrotempus.modules.features.glow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.modules.features.glow.GlowingEntityRegistry;

@SideOnly(Side.CLIENT)
public class GlowClientManager {

    private static final GlowClientManager INSTANCE = new GlowClientManager();

    private final GlowingEntityRegistry registry = new GlowingEntityRegistry();
    private KeyBinding debugKey;
    private boolean debugKeyWasDown = false;

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
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        registry.tick();
        handleDebugKey();
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
            MovingObjectPosition mop = mc.objectMouseOver;
            if (mop != null && mop.entityHit != null) {
                Entity e = mop.entityHit;
                registry.set(e.getEntityId(), 0xFFFFFF, 200);
                mc.thePlayer.addChatMessage(new ChatComponentText("Glowing entity " + e.getEntityId()));
            }
        }
        debugKeyWasDown = down;
    }
}
