package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.ChatComponentTranslation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.PreJoinPacket;
import io.github.cruciblemc.necrotempus.modules.features.dialog.prejoin.PreJoinNetwork;

@SideOnly(Side.CLIENT)
public final class PreJoinClientConnection implements DialogScreenContext {

    static final PreJoinClientConnection INSTANCE = new PreJoinClientConnection();

    private NetHandlerPlayClient handler;
    private NetHandlerPlayClient completedHandler;
    private UUID sessionId;
    private UUID lastSessionId;
    private int revision;
    private int lastRevision;
    private GuiDialogScreen dialogScreen;
    private GuiDialogWarningScreen warningScreen;
    private boolean capabilitySent;
    private boolean opened;
    private boolean submitted;
    private boolean changingScreen;
    private boolean joined;
    private boolean terminal;
    private boolean disconnectHandled;

    private PreJoinClientConnection() {}

    public static void receive(PreJoinPacket packet, NetHandlerPlayClient packetHandler) {
        if (packet == null || packetHandler == null || !packet.isValidFor(Side.CLIENT)) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.func_152344_a(() -> INSTANCE.handle(packet, packetHandler, minecraft));
    }

    public static void joined(NetHandlerPlayClient packetHandler) {
        Minecraft minecraft = Minecraft.getMinecraft();
        Runnable task = () -> INSTANCE.finishJoin(packetHandler);
        if (minecraft.func_152345_ab()) task.run();
        else minecraft.func_152344_a(task);
    }

    static boolean acceptsShow(UUID lastSessionId, int lastRevision, UUID nextSessionId, int nextRevision) {
        return nextSessionId != null && nextRevision > 0
            && (lastSessionId == null || (lastSessionId.equals(nextSessionId) && nextRevision > lastRevision));
    }

    static boolean canBegin(boolean hasHandler, boolean sameHandler, boolean currentChannelOpen,
        boolean completedHandler) {
        return !completedHandler && (!hasHandler || sameHandler || !currentChannelOpen);
    }

    static boolean canReceive(boolean sameHandler, boolean joinedOrTerminal, boolean channelOpen) {
        return sameHandler && !joinedOrTerminal && channelOpen;
    }

    static boolean canFinishJoin(boolean sameHandler) {
        return sameHandler;
    }

    static boolean canSubmit(UUID sessionId, int revision, UUID submittedSessionId, int submittedRevision,
        boolean opened, boolean submitted, boolean terminal, Dialog.AfterAction afterAction) {
        return sessionId != null && sessionId.equals(submittedSessionId)
            && revision == submittedRevision
            && opened
            && !submitted
            && !terminal
            && afterAction == Dialog.AfterAction.WAIT_FOR_RESPONSE;
    }

    private void handle(PreJoinPacket packet, NetHandlerPlayClient packetHandler, Minecraft minecraft) {
        if (packet.getOperation() == PreJoinPacket.Operation.HELLO) {
            begin(packetHandler, minecraft);
            return;
        }
        if (!canReceive(packetHandler == handler, joined || terminal, isChannelOpen(packetHandler))) return;
        switch (packet.getOperation()) {
            case SHOW:
                show(packet, minecraft);
                break;
            case CLEAR:
                clear(packet, minecraft);
                break;
            default:
                break;
        }
    }

    private void begin(NetHandlerPlayClient packetHandler, Minecraft minecraft) {
        if (!isChannelOpen(packetHandler) || !canBegin(
            handler != null,
            packetHandler == handler,
            isChannelOpen(handler),
            packetHandler == completedHandler)) return;
        if (packetHandler != handler) {
            reset();
            handler = packetHandler;
        }
        if (joined || terminal || capabilitySent) return;
        capabilitySent = true;
        if (!send(PreJoinPacket.capability(PreJoinPacket.PROTOCOL_VERSION))) {
            disconnect();
            return;
        }
        if (!showWaiting(minecraft, null, 0)) disconnect();
    }

    private void show(PreJoinPacket packet, Minecraft minecraft) {
        if (!acceptsShow(lastSessionId, lastRevision, packet.getSessionId(), packet.getRevision())) return;
        try {
            io.github.cruciblemc.necrotempus.modules.features.dialog.PreJoinDialogPolicy.validate(packet.getDialog());
        } catch (IllegalArgumentException exception) {
            disconnect();
            return;
        }
        lastSessionId = packet.getSessionId();
        lastRevision = packet.getRevision();
        sessionId = lastSessionId;
        revision = lastRevision;
        opened = false;
        submitted = false;
        dialogScreen = new GuiDialogScreen(packet.getDialog(), sessionId, revision, this);
        showScreen(minecraft, dialogScreen);
        if (minecraft.currentScreen == dialogScreen) opened(sessionId, revision);
        else disconnect();
    }

    private void clear(PreJoinPacket packet, Minecraft minecraft) {
        if (!matches(packet.getSessionId(), packet.getRevision())) return;
        sessionId = null;
        revision = 0;
        opened = false;
        submitted = false;
        dialogScreen = null;
        warningScreen = null;
        if (!showWaiting(minecraft, null, 0)) disconnect();
    }

    private boolean showWaiting(Minecraft minecraft, UUID id, int screenRevision) {
        GuiScreen screen = id == null ? new GuiWaitingForResponseScreen(this)
            : new GuiWaitingForResponseScreen(id, screenRevision, this, true);
        showScreen(minecraft, screen);
        return minecraft.currentScreen == screen;
    }

    private void showScreen(Minecraft minecraft, GuiScreen screen) {
        if (minecraft.currentScreen == screen) return;
        changingScreen = true;
        try {
            minecraft.displayGuiScreen(screen);
        } finally {
            changingScreen = false;
        }
    }

    private boolean send(PreJoinPacket packet) {
        NetHandlerPlayClient current = handler;
        if (current == null || !isChannelOpen(current)) return false;
        try {
            current.addToSendQueue(PreJoinNetwork.getPacket(packet));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isChannelOpen(NetHandlerPlayClient current) {
        return current != null && current.getNetworkManager() != null
            && current.getNetworkManager()
                .isChannelOpen();
    }

    private boolean matches(UUID id, int screenRevision) {
        return sessionId != null && sessionId.equals(id) && revision == screenRevision;
    }

    private void disconnect() {
        if (handler == null || joined || terminal) return;
        send(PreJoinPacket.disconnect());
        terminal = true;
        handler.getNetworkManager()
            .closeChannel(new ChatComponentTranslation("necrotempus.prejoin.client_disconnected"));
    }

    private void finishJoin(NetHandlerPlayClient packetHandler) {
        if (!canFinishJoin(handler == packetHandler)) return;
        completedHandler = packetHandler;
        joined = true;
        terminal = true;
        handler = null;
        clearDialogState();
    }

    private void reset() {
        handler = null;
        sessionId = null;
        lastSessionId = null;
        revision = 0;
        lastRevision = 0;
        dialogScreen = null;
        warningScreen = null;
        capabilitySent = false;
        opened = false;
        submitted = false;
        changingScreen = false;
        joined = false;
        terminal = false;
        disconnectHandled = false;
    }

    private void clearDialogState() {
        sessionId = null;
        revision = 0;
        dialogScreen = null;
        warningScreen = null;
        opened = false;
        submitted = false;
    }

    @Override
    public void tick() {
        NetHandlerPlayClient current = handler;
        if (current == null || joined || disconnectHandled) return;
        if (!terminal) PreJoinPanorama.tick();
        NetworkManager manager = current.getNetworkManager();
        if (manager.isChannelOpen()) {
            if (!terminal) manager.processReceivedPackets();
            return;
        }
        if (disconnectHandled) return;
        disconnectHandled = true;
        terminal = true;
        ChatComponentTranslation reason = new ChatComponentTranslation("disconnect.genericReason", "Disconnected");
        current.onDisconnect(manager.getExitMessage() == null ? reason : manager.getExitMessage());
        handler = null;
        clearDialogState();
    }

    @Override
    public void drawBackground(GuiScreen screen, float partialTicks) {
        PreJoinPanorama.draw(screen, partialTicks);
    }

    @Override
    public void opened(UUID id, int screenRevision) {
        if (!matches(id, screenRevision) || opened || submitted || terminal) return;
        opened = true;
        if (!send(PreJoinPacket.opened(id, screenRevision))) disconnect();
    }

    @Override
    public boolean action(UUID id, int screenRevision, int index, Map<String, Object> values,
        Dialog.AfterAction afterAction) {
        if (!canSubmit(sessionId, revision, id, screenRevision, opened, submitted, terminal, afterAction)) return false;
        submitted = true;
        if (!send(PreJoinPacket.submit(id, screenRevision, index, values))) {
            disconnect();
            return false;
        }
        if (!showWaiting(Minecraft.getMinecraft(), id, screenRevision)) {
            disconnect();
            return false;
        }
        return true;
    }

    @Override
    public boolean cancel(UUID id, int screenRevision, Map<String, Object> values) {
        if (!matches(id, screenRevision) || terminal) return false;
        disconnect();
        return true;
    }

    @Override
    public boolean showConfirmation(UUID id, int screenRevision, GuiScreen confirmation) {
        return false;
    }

    @Override
    public boolean showWarning(UUID id, int screenRevision) {
        if (!matches(id, screenRevision) || dialogScreen == null || terminal) return false;
        warningScreen = new GuiDialogWarningScreen(dialogScreen, this);
        showScreen(Minecraft.getMinecraft(), warningScreen);
        return Minecraft.getMinecraft().currentScreen == warningScreen;
    }

    @Override
    public void restoreDialog(UUID id, int screenRevision) {
        if (matches(id, screenRevision) && dialogScreen != null && !terminal)
            showScreen(Minecraft.getMinecraft(), dialogScreen);
    }

    @Override
    public void dismissWaiting(UUID id, int screenRevision) {
        if ((id == null && sessionId == null && screenRevision == 0) || matches(id, screenRevision)) disconnect();
    }

    @Override
    public void screenClosed(UUID id, int screenRevision) {
        if (changingScreen || joined || terminal || handler == null) return;
        if (matches(id, screenRevision) || (id == null && sessionId == null && screenRevision == 0)) disconnect();
    }

    @Override
    public void leaveWorld() {
        disconnect();
    }

    @Override
    public void returnFromWarning(GuiDialogWarningScreen warning) {
        if (warningScreen != warning) return;
        GuiScreen destination = warning.getReturnScreen();
        warningScreen = null;
        if (destination == null) {
            if (!showWaiting(Minecraft.getMinecraft(), null, 0)) disconnect();
        } else showScreen(Minecraft.getMinecraft(), destination);
    }

    @Override
    public void warningClosed(GuiDialogWarningScreen warning) {
        if (!changingScreen && !joined && !terminal && warningScreen == warning) disconnect();
    }
}
