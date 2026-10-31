package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.function.BiConsumer;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogBodyActions;
import io.github.cruciblemc.necrotempus.api.dialog.DialogManager;
import io.github.cruciblemc.necrotempus.api.dialog.DialogResponse;
import io.github.cruciblemc.necrotempus.modules.features.dialog.DialogFormValues;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.DialogMenuRequestPacket;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.DialogPacket;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.DialogResponsePacket;

public final class DialogSessions {

    private static final long OPEN_TIMEOUT_NANOS = 30_000_000_000L;
    private static final int MAX_PENDING_PER_PLAYER = 16;
    private static final int MAX_PENDING_GLOBAL = 1024;
    private static final int MAX_DRAIN_PER_TICK = 256;
    private static final DialogSessions INSTANCE = new DialogSessions();
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Queue<Pending>> pending = new LinkedHashMap<>();
    private final Map<UUID, Overflow> overflowed = new HashMap<>();
    private int pendingCount;

    private DialogSessions() {}

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    public static DialogManager.ShowResult show(EntityPlayerMP player, Dialog dialog,
        BiConsumer<EntityPlayerMP, DialogResponse> callback) {
        UUID id = UUID.randomUUID();
        int revision = 1;
        int controls = controlCount(dialog);
        int exitIndex = dialog.getExitAction() == null ? -1 : controls;
        DialogSession state = new DialogSession(
            id,
            revision,
            actionUseLimits(dialog, controls, exitIndex),
            dialog.getCancelActionIndex(),
            dialog.canCloseWithEscape());
        Session session = new Session(player, id, dialog, state, System.nanoTime(), callback);
        Session previous = INSTANCE.sessions.put(player.getUniqueID(), session);
        if (previous != null) {
            NecroTempus.DISPATCHER.sendTo(DialogPacket.clear(previous.id, previous.revision), player);
        }
        NecroTempus.DISPATCHER.sendTo(DialogPacket.show(id, revision, dialog), player);
        return DialogManager.ShowResult.SHOWN;
    }

    public static void clear(EntityPlayerMP player) {
        if (player == null) return;
        Session session = INSTANCE.sessions.remove(player.getUniqueID());
        if (session == null) return;
        session.state.closeByOtherGui(session.id, session.revision);
        NecroTempus.DISPATCHER.sendTo(DialogPacket.clear(session.id, session.revision), player);
    }

    public static void handle(EntityPlayerMP player, DialogResponsePacket response) {
        Session session = INSTANCE.sessions.get(player.getUniqueID());
        if (session == null || !session.id.equals(response.getSessionId())
            || session.revision != response.getRevision()) {
            return;
        }

        switch (response.getOperation()) {
            case OPENED:
                session.state.acknowledgeOpened(session.id, session.revision);
                return;
            case ACTION:
                Map<String, Object> actionValues;
                try {
                    actionValues = valuesForAction(
                        session.dialog,
                        actionFor(session.dialog, response.getActionIndex()),
                        response.getValues());
                } catch (IllegalArgumentException exception) {
                    INSTANCE.reject(player, session);
                    return;
                }
                session.state
                    .acceptAction(
                        session.id,
                        session.revision,
                        response.getActionIndex(),
                        session.dialog.getAfterAction())
                    .ifPresent(
                        index -> INSTANCE
                            .complete(player, session, index, false, actionValues, session.dialog.getAfterAction()));
                return;
            case CANCEL:
                Map<String, Object> cancelValues;
                try {
                    cancelValues = valuesForAction(
                        session.dialog,
                        session.dialog.getCancelAction() == null ? null
                            : session.dialog.getCancelAction()
                                .getAction(),
                        response.getValues());
                } catch (IllegalArgumentException exception) {
                    INSTANCE.reject(player, session);
                    return;
                }
                session.state.acceptCancel(session.id, session.revision)
                    .ifPresent(
                        index -> INSTANCE
                            .complete(player, session, index, true, cancelValues, Dialog.AfterAction.CLOSE));
                return;
            case CLOSED_BY_OTHER_GUI:
                if (session.state.closeByOtherGui(session.id, session.revision)) {
                    INSTANCE.sessions.remove(player.getUniqueID());
                }
                return;
            case REJECTED:
                if (session.state.reject(session.id, session.revision)) {
                    INSTANCE.sessions.remove(player.getUniqueID());
                }
                return;
            case DISMISS:
                if (session.state.dismissWaiting(session.id, session.revision)) {
                    INSTANCE.sessions.remove(player.getUniqueID());
                }
        }
    }

    public static void enqueue(EntityPlayerMP player, DialogResponsePacket response) {
        UUID playerId = player.getUniqueID();
        synchronized (INSTANCE) {
            Queue<Pending> playerQueue = INSTANCE.pending.get(playerId);
            if (INSTANCE.overflowed.containsKey(playerId)) return;
            if ((playerQueue != null && playerQueue.size() >= MAX_PENDING_PER_PLAYER)
                || INSTANCE.pendingCount >= MAX_PENDING_GLOBAL) {
                INSTANCE.overflowed.put(playerId, new Overflow(response.getSessionId(), response.getRevision()));
                return;
            }
            if (playerQueue == null) {
                playerQueue = new ArrayDeque<>();
                INSTANCE.pending.put(playerId, playerQueue);
            }
            playerQueue.add(new Pending(player, response));
            INSTANCE.pendingCount++;
        }
    }

    public static void enqueueMenu(EntityPlayerMP player, DialogMenuRequestPacket request) {
        DialogMenuService.enqueue(player, request);
    }

    private void complete(EntityPlayerMP player, Session session, int index, boolean cancelled,
        Map<String, Object> values, Dialog.AfterAction afterAction) {
        Dialog.Action selectedAction = actionFor(session.dialog, index);
        if (selectedAction != null && selectedAction.getType() == Dialog.ActionType.SHOW_DIALOG) {
            Dialog destination = resolveActionDestination(session.dialog, index);
            DialogManager.ShowResult result = destination == null ? DialogManager.ShowResult.UNSUPPORTED_DIALOG
                : DialogManager.show(player, destination, session.callback);
            if (result != DialogManager.ShowResult.SHOWN) {
                sessions.remove(player.getUniqueID(), session);
                NecroTempus.DISPATCHER.sendTo(DialogPacket.clear(session.id, session.revision), player);
                player.addChatMessage(new ChatComponentText("Dialog could not open: " + result.name()));
            }
            return;
        }
        if (cancelled || afterAction == Dialog.AfterAction.CLOSE) sessions.remove(player.getUniqueID());
        if (!cancelled && session.dialog.getType() == Dialog.Type.DIALOG_LIST
            && index >= 0
            && index < session.dialog.getEntries()
                .size()) {
            Dialog destination = resolveListDestination(session.dialog, index);
            if (destination == null) {
                player.addChatMessage(new ChatComponentText("Dialog list target is not registered."));
                return;
            }
            DialogManager.ShowResult result = DialogManager.show(player, destination, session.callback);
            if (result != DialogManager.ShowResult.SHOWN) {
                player.addChatMessage(new ChatComponentText("Dialog could not open: " + result.name()));
            }
            return;
        }
        if (selectedAction != null && selectedAction.getType() == Dialog.ActionType.RUN_COMMAND) {
            runCommand(player, session.dialog, selectedAction, values);
        }
        if (session.callback == null || !invokesCallback(selectedAction)) return;
        try {
            session.callback.accept(player, new DialogResponse(index, cancelled, values, selectedAction));
        } catch (RuntimeException exception) {
            NecroTempus.getInstance()
                .getLogger()
                .error("Dialog callback failed", exception);
        }
    }

    static Dialog resolveListDestination(Dialog dialog, int index) {
        if (dialog == null || dialog.getType() != Dialog.Type.DIALOG_LIST
            || index < 0
            || index >= dialog.getEntries()
                .size())
            return null;
        Dialog.Entry entry = dialog.getEntries()
            .get(index);
        if (entry.getInlineDialog() != null) return entry.getInlineDialog();
        return entry.getDialogId() == null ? null : DialogManager.getRegistered(entry.getDialogId());
    }

    static Dialog resolveActionDestination(Dialog dialog, int index) {
        Dialog.Action action = actionFor(dialog, index);
        if (action == null || action.getType() != Dialog.ActionType.SHOW_DIALOG) return null;
        return action.getInlineDialog() == null ? DialogManager.getRegistered(action.getValue())
            : action.getInlineDialog();
    }

    static Dialog.Action actionFor(Dialog dialog, int index) {
        if (dialog == null || index < 0) return null;
        int controls = controlCount(dialog);
        if (dialog.getType() == Dialog.Type.SERVER_LINKS) {
            if (index < controls) {
                return Dialog.Action.openUrl(
                    dialog.getServerLinks()
                        .get(index)
                        .getUrl());
            }
        } else if (dialog.getType() != Dialog.Type.DIALOG_LIST && index < dialog.getActions()
            .size()) {
                return dialog.getActions()
                    .get(index)
                    .getAction();
            }
        if (index == controls && dialog.getExitAction() != null) {
            return dialog.getExitAction()
                .getAction();
        }
        int bodyIndex = index - controls - (dialog.getExitAction() == null ? 0 : 1);
        List<Dialog.Action> bodyActions = DialogBodyActions.collect(dialog);
        return bodyIndex < 0 || bodyIndex >= bodyActions.size() ? null : bodyActions.get(bodyIndex);
    }

    static boolean invokesCallback(Dialog.Action action) {
        if (action == null) return true;
        Dialog.ActionType type = action.getType();
        return type != Dialog.ActionType.SUGGEST_COMMAND && type != Dialog.ActionType.COPY_TO_CLIPBOARD
            && type != Dialog.ActionType.OPEN_URL
            && type != Dialog.ActionType.SHOW_DIALOG;
    }

    static Map<String, Object> valuesForAction(Dialog dialog, Dialog.Action action, Map<String, Object> values) {
        if (action != null && action.isDynamic()
            && (action.getType() == Dialog.ActionType.CUSTOM || action.getType() == Dialog.ActionType.RUN_COMMAND)) {
            return DialogFormValues.validate(dialog, values);
        }
        if (values == null || !values.isEmpty()) {
            throw new IllegalArgumentException("This dialog action does not accept input values");
        }
        return Collections.emptyMap();
    }

    private static int controlCount(Dialog dialog) {
        if (dialog.getType() == Dialog.Type.DIALOG_LIST) return dialog.getEntries()
            .size();
        if (dialog.getType() == Dialog.Type.SERVER_LINKS) return dialog.getServerLinks()
            .size();
        return dialog.getActions()
            .size();
    }

    static int[] actionUseLimits(Dialog dialog, int controls, int exitIndex) {
        List<Dialog.Action> bodyActions = DialogBodyActions.collect(dialog);
        int[] limits = new int[controls + (exitIndex < 0 ? 0 : 1) + bodyActions.size()];
        if (dialog.getType() == Dialog.Type.DIALOG_LIST || dialog.getType() == Dialog.Type.SERVER_LINKS) {
            for (int i = 0; i < controls; i++) limits[i] = 1;
        } else {
            for (int i = 0; i < controls; i++) {
                limits[i] = dialog.getActions()
                    .get(i)
                    .getAction()
                    .getMaxUses();
            }
        }
        if (exitIndex >= 0) {
            limits[exitIndex] = dialog.getExitAction()
                .getAction()
                .getMaxUses();
        }
        for (int i = 0; i < bodyActions.size(); i++) {
            limits[controls + (exitIndex < 0 ? 0 : 1) + i] = bodyActions.get(i)
                .getMaxUses();
        }
        return limits;
    }

    private static void runCommand(EntityPlayerMP player, Dialog dialog, Dialog.Action action,
        Map<String, Object> values) {
        String command = action.resolveCommand(dialog, values);
        if (command.startsWith("/")) command = command.substring(1);
        if (command.isEmpty()) return;
        try {
            MinecraftServer.getServer()
                .getCommandManager()
                .executeCommand(player, command);
        } catch (RuntimeException exception) {
            NecroTempus.getInstance()
                .getLogger()
                .error("Dialog command failed", exception);
        }
    }

    private void reject(EntityPlayerMP player, Session session) {
        if (!session.state.reject(session.id, session.revision)) return;
        sessions.remove(player.getUniqueID());
        NecroTempus.DISPATCHER.sendTo(DialogPacket.clear(session.id, session.revision), player);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        drainResponses();
        DialogMenuService.drainRequests();
        long now = System.nanoTime();
        Iterator<Map.Entry<UUID, Session>> iterator = sessions.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next()
                .getValue();
            if (!session.state.isPending() || now - session.createdAt < OPEN_TIMEOUT_NANOS) continue;
            iterator.remove();
            NecroTempus.DISPATCHER.sendTo(DialogPacket.clear(session.id, session.revision), session.player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            UUID playerId = event.player.getUniqueID();
            sessions.remove(playerId);
            DialogMenuService.logout((EntityPlayerMP) event.player);
            synchronized (this) {
                Queue<Pending> removed = pending.remove(playerId);
                if (removed != null) pendingCount -= removed.size();
                overflowed.remove(playerId);
            }
        }
    }

    private void drainResponses() {
        List<Pending> work = new ArrayList<>();
        Map<UUID, Overflow> dropped;
        synchronized (this) {
            dropped = new HashMap<>(overflowed);
            overflowed.clear();
            for (UUID playerId : dropped.keySet()) {
                Queue<Pending> removed = pending.remove(playerId);
                if (removed != null) pendingCount -= removed.size();
            }
            while (work.size() < MAX_DRAIN_PER_TICK && !pending.isEmpty()) {
                Map.Entry<UUID, Queue<Pending>> first = pending.entrySet()
                    .iterator()
                    .next();
                UUID playerId = first.getKey();
                Queue<Pending> queue = first.getValue();
                work.add(queue.remove());
                pendingCount--;
                pending.remove(playerId);
                if (!queue.isEmpty()) pending.put(playerId, queue);
            }
        }
        for (Map.Entry<UUID, Overflow> entry : dropped.entrySet()) {
            UUID playerId = entry.getKey();
            Overflow marker = entry.getValue();
            Session current = sessions.get(playerId);
            if (current != null && current.matches(marker.sessionId, marker.revision)) {
                sessions.remove(playerId);
                current.state.closeByOtherGui(marker.sessionId, marker.revision);
                NecroTempus.DISPATCHER.sendTo(DialogPacket.clear(current.id, current.revision), current.player);
            }
        }
        for (Pending pendingResponse : work) handle(pendingResponse.player, pendingResponse.response);
    }

    private static final class Session {

        private final EntityPlayerMP player;
        private final UUID id;
        private final Dialog dialog;
        private final int revision = 1;
        private final DialogSession state;
        private final long createdAt;
        private final BiConsumer<EntityPlayerMP, DialogResponse> callback;

        private Session(EntityPlayerMP player, UUID id, Dialog dialog, DialogSession state, long createdAt,
            BiConsumer<EntityPlayerMP, DialogResponse> callback) {
            this.player = player;
            this.id = id;
            this.dialog = dialog;
            this.state = state;
            this.createdAt = createdAt;
            this.callback = callback;
        }

        private boolean matches(UUID sessionId, int sessionRevision) {
            return id.equals(sessionId) && revision == sessionRevision
                && state.matchesSession(sessionId, sessionRevision);
        }

    }

    private static final class Pending {

        private final EntityPlayerMP player;
        private final DialogResponsePacket response;

        private Pending(EntityPlayerMP player, DialogResponsePacket response) {
            this.player = player;
            this.response = response;
        }
    }

    private static final class Overflow {

        private final UUID sessionId;
        private final int revision;

        private Overflow(UUID sessionId, int revision) {
            this.sessionId = sessionId;
            this.revision = revision;
        }
    }
}
