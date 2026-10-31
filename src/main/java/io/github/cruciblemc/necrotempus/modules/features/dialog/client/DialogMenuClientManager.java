package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.api.dialog.DialogMenuCatalog;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.DialogMenuRequestPacket;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.DialogMenuStatePacket;

@SideOnly(Side.CLIENT)
public final class DialogMenuClientManager {

    private static final int PAUSE_BUTTON_ID = 0x4E5401;
    private static final DialogMenuClientManager INSTANCE = new DialogMenuClientManager();

    private final KeyBinding quickActionKey = new KeyBinding(
        "key.necrotempus.dialogs.quick_actions",
        Keyboard.KEY_G,
        "NecroTempus");
    private int revision = -1;
    private int pauseCount;
    private int quickActionCount;
    private int serverLinkCount;
    private String pauseButtonLabel;

    private DialogMenuClientManager() {}

    public static void register() {
        ClientRegistry.registerKeyBinding(INSTANCE.quickActionKey);
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public static void apply(DialogMenuStatePacket packet) {
        if (packet.getRevision() < INSTANCE.revision) return;
        INSTANCE.revision = packet.getRevision();
        INSTANCE.pauseCount = packet.getPauseCount();
        INSTANCE.quickActionCount = packet.getQuickActionCount();
        INSTANCE.serverLinkCount = packet.getServerLinkCount();
        INSTANCE.pauseButtonLabel = packet.getPauseLabel() == null ? null
            : packet.getPauseLabel()
                .getFormattedText();
    }

    public static void reset() {
        INSTANCE.revision = -1;
        INSTANCE.pauseCount = 0;
        INSTANCE.quickActionCount = 0;
        INSTANCE.serverLinkCount = 0;
        INSTANCE.pauseButtonLabel = null;
    }

    @SubscribeEvent
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiIngameMenu) || pauseCount == 0 && serverLinkCount == 0) return;
        int y = event.gui.height / 4 + 144;
        List<GuiButton> buttons = (List<GuiButton>) event.buttonList;
        buttons.add(
            new GuiButton(
                PAUSE_BUTTON_ID,
                event.gui.width / 2 - 100,
                y,
                200,
                20,
                pauseButtonLabel == null ? I18n.format("gui.necrotempus.dialogs.pause") : pauseButtonLabel));
    }

    @SubscribeEvent
    public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.gui instanceof GuiIngameMenu) || event.button.id != PAUSE_BUTTON_ID) return;
        event.setCanceled(true);
        open(DialogMenuCatalog.Kind.PAUSE_SCREEN);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        boolean pressed = quickActionKey.isPressed();
        if (!pressed || quickActionCount == 0 || revision < 1) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer != null && minecraft.theWorld != null && minecraft.currentScreen == null) {
            open(DialogMenuCatalog.Kind.QUICK_ACTION);
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        reset();
    }

    private void open(DialogMenuCatalog.Kind kind) {
        if (revision < 1) return;
        int count = kind == DialogMenuCatalog.Kind.PAUSE_SCREEN ? pauseCount
            : kind == DialogMenuCatalog.Kind.QUICK_ACTION ? quickActionCount : serverLinkCount;
        if (kind == DialogMenuCatalog.Kind.PAUSE_SCREEN && count == 0) count = serverLinkCount;
        if (count == 0) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        NecroTempus.DISPATCHER.sendToServer(new DialogMenuRequestPacket(kind, revision));
    }
}
