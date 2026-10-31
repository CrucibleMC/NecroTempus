package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogManager;
import io.github.cruciblemc.necrotempus.api.dialog.DialogMenuCatalog;
import io.github.cruciblemc.necrotempus.modules.features.dialog.DialogCodec;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.DialogMenuRequestPacket;
import io.github.cruciblemc.necrotempus.modules.features.dialog.network.DialogMenuStatePacket;
import io.github.cruciblemc.necrotempus.utils.ServerUtils;

public final class DialogMenuService {

    private static final int MAX_PENDING_PER_PLAYER = 8;
    private static final int MAX_PENDING_GLOBAL = 256;
    private static final int MAX_DRAIN_PER_TICK = 64;
    private static final Map<UUID, DialogMenuCatalog> CATALOGS = new ConcurrentHashMap<>();
    private static final Map<UUID, TagMenuSelection> TAG_SELECTIONS = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> TAGS = new ConcurrentHashMap<>();
    private static final Queue<Pending> REQUESTS = new ArrayDeque<>();
    private static final Map<UUID, Integer> PENDING_BY_PLAYER = new HashMap<>();

    private DialogMenuService() {}

    public static List<String> registerTag(String id, List<String> dialogIds) {
        DialogMenuCatalog validated = new DialogMenuCatalog(1, Collections.singletonList(id), dialogIds);
        List<String> replacement = validated.getQuickActionIds();
        List<String> previous = TAGS.put(id, replacement);
        try {
            refreshTagSelections(id);
        } catch (IllegalArgumentException exception) {
            if (previous == null) TAGS.remove(id);
            else TAGS.put(id, previous);
            throw exception;
        }
        return previous;
    }

    public static boolean unregisterTag(String id) {
        new DialogMenuCatalog(1, Collections.singletonList(id), Collections.emptyList());
        if (TAGS.remove(id) == null) return false;
        refreshTagSelections(id);
        return true;
    }

    public static void setMenuTags(EntityPlayerMP player, List<String> pauseTagIds, List<String> quickTagIds) {
        if (player == null) throw new IllegalArgumentException("Player is required");
        DialogMenuCatalog selection = new DialogMenuCatalog(1, pauseTagIds, quickTagIds);
        expandTags(selection.getPauseScreenIds());
        expandTags(selection.getQuickActionIds());
        DialogMenuCatalog catalog = menuCatalog(
            player,
            resolveMenuTagIds(selection.getPauseScreenIds()),
            resolveMenuTagIds(selection.getQuickActionIds()));
        TAG_SELECTIONS.put(player.getUniqueID(), new TagMenuSelection(player, selection));
        CATALOGS.put(player.getUniqueID(), catalog);
        sync(player);
    }

    static List<String> expandTags(List<String> tagIds) {
        DialogMenuCatalog validated = new DialogMenuCatalog(1, tagIds, Collections.emptyList());
        Set<String> result = new LinkedHashSet<>();
        for (String tagId : validated.getPauseScreenIds()) {
            List<String> entries = TAGS.get(tagId);
            if (entries == null) throw new IllegalArgumentException("Dialog tag is not registered: " + tagId);
            result.addAll(entries);
            if (result.size() > DialogMenuCatalog.MAX_MENU_ENTRIES) {
                throw new IllegalArgumentException("Expanded dialog menu exceeds its entry limit");
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(result));
    }

    static List<String> resolveMenuTagIds(List<String> tagIds) {
        Set<String> result = new LinkedHashSet<>();
        for (String tagId : tagIds) {
            List<String> entries = TAGS.get(tagId);
            if (entries != null) result.addAll(entries);
            if (result.size() > DialogMenuCatalog.MAX_MENU_ENTRIES) {
                throw new IllegalArgumentException("Expanded dialog menu exceeds its entry limit");
            }
        }
        return availableIds(new ArrayList<>(result));
    }

    public static void setMenu(EntityPlayerMP player, List<String> pauseIds, List<String> quickActionIds) {
        if (player == null) throw new IllegalArgumentException("Player is required");
        DialogMenuCatalog catalog = menuCatalog(player, pauseIds, quickActionIds);
        TAG_SELECTIONS.remove(player.getUniqueID());
        CATALOGS.put(player.getUniqueID(), catalog);
        sync(player);
    }

    private static DialogMenuCatalog menuCatalog(EntityPlayerMP player, List<String> pauseIds,
        List<String> quickActionIds) {
        DialogMenuCatalog previous = CATALOGS.get(player.getUniqueID());
        DialogMenuCatalog catalog = new DialogMenuCatalog(
            previous == null ? 1 : incrementRevision(previous.getRevision()),
            pauseIds,
            quickActionIds,
            previous == null ? Collections.emptyList() : previous.getServerLinks());
        requireRegistered(catalog.getPauseScreenIds());
        requireRegistered(catalog.getQuickActionIds());
        return catalog;
    }

    public static void setServerLinks(EntityPlayerMP player, List<Dialog.ServerLink> links) {
        if (player == null) throw new IllegalArgumentException("Player is required");
        DialogMenuCatalog previous = CATALOGS.get(player.getUniqueID());
        int revision = previous == null ? 1 : incrementRevision(previous.getRevision());
        DialogMenuCatalog catalog = new DialogMenuCatalog(
            revision,
            previous == null ? Collections.emptyList() : previous.getPauseScreenIds(),
            previous == null ? Collections.emptyList() : previous.getQuickActionIds(),
            links);
        if (!catalog.getServerLinks()
            .isEmpty()) {
            DialogCodec.requireSupported(
                resolveServerLinks(catalog.getServerLinks(), "gui.necrotempus.dialogs.server_links_title"));
        }
        CATALOGS.put(player.getUniqueID(), catalog);
        sync(player);
    }

    public static void sync(EntityPlayerMP player) {
        if (player == null || !DialogManager.supports(player)) return;
        DialogMenuCatalog catalog = CATALOGS.get(player.getUniqueID());
        int revision = catalog == null ? 0 : catalog.getRevision();
        List<String> pauseIds = catalog == null ? Collections.emptyList()
            : menuIds(player, catalog, DialogMenuCatalog.Kind.PAUSE_SCREEN);
        List<String> quickIds = catalog == null ? Collections.emptyList()
            : menuIds(player, catalog, DialogMenuCatalog.Kind.QUICK_ACTION);
        int pauseCount = pauseIds.size();
        int quickCount = quickIds.size();
        int serverLinkCount = catalog == null ? 0
            : catalog.getServerLinks()
                .size();
        net.minecraft.util.IChatComponent pauseLabel = catalog == null ? null
            : pauseLabel(pauseIds, catalog.getServerLinks());
        NecroTempus.DISPATCHER
            .sendTo(new DialogMenuStatePacket(revision, pauseCount, quickCount, serverLinkCount, pauseLabel), player);
    }

    public static void enqueue(EntityPlayerMP player, DialogMenuRequestPacket request) {
        UUID playerId = player.getUniqueID();
        synchronized (REQUESTS) {
            int count = PENDING_BY_PLAYER.containsKey(playerId) ? PENDING_BY_PLAYER.get(playerId) : 0;
            if (count >= MAX_PENDING_PER_PLAYER || REQUESTS.size() >= MAX_PENDING_GLOBAL) return;
            REQUESTS.add(new Pending(player, request));
            PENDING_BY_PLAYER.put(playerId, count + 1);
        }
    }

    public static void drainRequests() {
        for (int i = 0; i < MAX_DRAIN_PER_TICK; i++) {
            Pending next;
            synchronized (REQUESTS) {
                next = REQUESTS.poll();
                if (next == null) return;
                decrementPending(next.player.getUniqueID());
            }
            if (next.player.playerNetServerHandler != null) open(next.player, next.request);
        }
    }

    public static void logout(EntityPlayerMP player) {
        UUID playerId = player.getUniqueID();
        CATALOGS.remove(playerId);
        TAG_SELECTIONS.remove(playerId);
        synchronized (REQUESTS) {
            Iterator<Pending> iterator = REQUESTS.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().player.getUniqueID()
                    .equals(playerId)) iterator.remove();
            }
            PENDING_BY_PLAYER.remove(playerId);
        }
    }

    static Dialog resolveMenu(List<String> ids, String title) {
        if (ids == null || ids.isEmpty()) return null;
        List<Map.Entry<String, Dialog>> available = new ArrayList<>();
        for (String id : ids) {
            Dialog dialog = DialogManager.getRegistered(id);
            if (dialog != null) available.add(new java.util.AbstractMap.SimpleImmutableEntry<>(id, dialog));
        }
        if (available.isEmpty()) return null;
        if (available.size() == 1) return available.get(0)
            .getValue();

        Dialog.Builder menu = Dialog.dialogList(new ChatComponentTranslation(title));
        for (Map.Entry<String, Dialog> entry : available) {
            Dialog dialog = entry.getValue();
            net.minecraft.util.IChatComponent label = dialog.getExternalTitle();
            if (label == null) label = dialog.getTitle();
            menu.entry(new Dialog.Entry(label, entry.getKey()));
        }
        return menu.build();
    }

    static Dialog resolvePauseMenu(List<String> ids, List<Dialog.ServerLink> links, String title) {
        Dialog menu = resolveMenu(ids, title);
        return menu == null ? resolveServerLinks(links, "gui.necrotempus.dialogs.server_links_title") : menu;
    }

    public static Dialog expandDialogListTags(Dialog dialog) {
        if (dialog == null || dialog.getType() != Dialog.Type.DIALOG_LIST) return dialog;
        boolean hasTag = false;
        for (Dialog.Entry entry : dialog.getEntries()) hasTag |= entry.isTagReference();
        if (!hasTag) return dialog;

        Dialog.Builder expanded = Dialog.dialogList(dialog.getTitle())
            .externalTitle(dialog.getExternalTitle())
            .columns(dialog.getColumns())
            .buttonWidth(dialog.getButtonWidth())
            .canCloseWithEscape(dialog.canCloseWithEscape())
            .pause(dialog.pausesGame())
            .commandExecutionPolicy(dialog.getCommandExecutionPolicy())
            .afterAction(dialog.getAfterAction());
        for (Dialog.Body body : dialog.getBody()) expanded.body(body);
        for (Dialog.Input input : dialog.getInputs()) expanded.input(input);
        for (Dialog.Entry entry : dialog.getEntries()) {
            if (!entry.isTagReference()) {
                expanded.entry(entry);
                continue;
            }
            List<String> ids = TAGS.get(entry.getTagId());
            if (ids == null) throw new IllegalArgumentException("Dialog tag is not registered: " + entry.getTagId());
            for (String id : ids) {
                Dialog target = DialogManager.getRegistered(id);
                if (target == null) continue;
                net.minecraft.util.IChatComponent label = target.getExternalTitle();
                if (label == null) label = target.getTitle();
                expanded.entry(new Dialog.Entry(label, id));
            }
        }
        if (dialog.getExitAction() != null) expanded.exitAction(dialog.getExitAction());
        return expanded.build();
    }

    static Dialog resolveServerLinks(List<Dialog.ServerLink> links, String title) {
        if (links == null || links.isEmpty()) return null;
        Dialog.Builder dialog = Dialog.serverLinks(new ChatComponentTranslation(title));
        for (Dialog.ServerLink link : links) dialog.serverLink(link);
        return dialog.build();
    }

    private static void open(EntityPlayerMP player, DialogMenuRequestPacket request) {
        DialogMenuCatalog catalog = CATALOGS.get(player.getUniqueID());
        if (catalog == null || catalog.getRevision() != request.getRevision()) {
            sync(player);
            return;
        }
        String title;
        Dialog dialog;
        if (request.getKind() == DialogMenuCatalog.Kind.SERVER_LINKS) {
            title = "gui.necrotempus.dialogs.server_links_title";
            dialog = resolveServerLinks(catalog.getServerLinks(), title);
        } else if (request.getKind() == DialogMenuCatalog.Kind.PAUSE_SCREEN) {
            title = "gui.necrotempus.dialogs.pause";
            dialog = resolvePauseMenu(
                menuIds(player, catalog, DialogMenuCatalog.Kind.PAUSE_SCREEN),
                catalog.getServerLinks(),
                title);
        } else {
            title = "gui.necrotempus.dialogs.quick_title";
            dialog = resolveMenu(menuIds(player, catalog, request.getKind()), title);
        }
        if (dialog == null) {
            sync(player);
            return;
        }
        DialogManager.ShowResult result = DialogManager.show(player, dialog, null);
        if (result != DialogManager.ShowResult.SHOWN) {
            player.addChatMessage(new ChatComponentText("Dialog menu could not open: " + result.name()));
        }
    }

    private static List<String> availableIds(List<String> ids) {
        if (ids.isEmpty()) return Collections.emptyList();
        List<String> available = new ArrayList<>(ids.size());
        for (String id : ids) if (DialogManager.getRegistered(id) != null) available.add(id);
        return available;
    }

    static net.minecraft.util.IChatComponent pauseLabel(List<String> ids, List<Dialog.ServerLink> links) {
        List<String> available = availableIds(ids);
        if (available.size() == 1) {
            Dialog dialog = DialogManager.getRegistered(available.get(0));
            return dialog.getExternalTitle() == null ? dialog.getTitle() : dialog.getExternalTitle();
        }
        if (available.size() > 1) return new ChatComponentTranslation("gui.necrotempus.dialogs.pause");
        return links.isEmpty() ? null : new ChatComponentTranslation("gui.necrotempus.dialogs.server_links");
    }

    private static List<String> menuIds(EntityPlayerMP player, DialogMenuCatalog catalog, DialogMenuCatalog.Kind kind) {
        TagMenuSelection selection = TAG_SELECTIONS.get(player.getUniqueID());
        if (selection == null) return availableIds(catalog.getIds(kind));
        return selection.resolve(kind);
    }

    private static void refreshTagSelections(String changedTag) {
        List<TagMenuSelection> affected = new ArrayList<>();
        for (TagMenuSelection selection : TAG_SELECTIONS.values()) {
            if (changedTag == null || selection.references(changedTag)) {
                selection.resolve(DialogMenuCatalog.Kind.PAUSE_SCREEN);
                selection.resolve(DialogMenuCatalog.Kind.QUICK_ACTION);
                affected.add(selection);
            }
        }
        for (TagMenuSelection selection : affected) sync(selection.player);
    }

    public static void refreshTagsForDialog(String dialogId) {
        for (TagMenuSelection selection : TAG_SELECTIONS.values()) {
            if (selection.containsDialog(dialogId)) sync(selection.player);
        }
        for (Map.Entry<UUID, DialogMenuCatalog> entry : CATALOGS.entrySet()) {
            if (TAG_SELECTIONS.containsKey(entry.getKey())) continue;
            DialogMenuCatalog catalog = entry.getValue();
            if (catalog.getPauseScreenIds()
                .contains(dialogId)
                || catalog.getQuickActionIds()
                    .contains(dialogId)) {
                sync(ServerUtils.getPlayer(entry.getKey()));
            }
        }
    }

    private static void requireRegistered(List<String> ids) {
        for (String id : ids) {
            if (DialogManager.getRegistered(id) == null) {
                throw new IllegalArgumentException("Dialog menu ID is not registered: " + id);
            }
        }
    }

    private static int incrementRevision(int revision) {
        if (revision == Integer.MAX_VALUE) throw new IllegalStateException("Dialog menu revision exhausted");
        return revision + 1;
    }

    private static void decrementPending(UUID playerId) {
        int count = PENDING_BY_PLAYER.get(playerId) - 1;
        if (count == 0) PENDING_BY_PLAYER.remove(playerId);
        else PENDING_BY_PLAYER.put(playerId, count);
    }

    private static final class Pending {

        private final EntityPlayerMP player;
        private final DialogMenuRequestPacket request;

        private Pending(EntityPlayerMP player, DialogMenuRequestPacket request) {
            this.player = player;
            this.request = request;
        }
    }

    private static final class TagMenuSelection {

        private final EntityPlayerMP player;
        private final DialogMenuCatalog catalog;

        private TagMenuSelection(EntityPlayerMP player, DialogMenuCatalog catalog) {
            this.player = player;
            this.catalog = catalog;
        }

        private boolean references(String tagId) {
            return catalog.getPauseScreenIds()
                .contains(tagId)
                || catalog.getQuickActionIds()
                    .contains(tagId);
        }

        private boolean containsDialog(String dialogId) {
            for (String tagId : catalog.getPauseScreenIds()) {
                List<String> entries = TAGS.get(tagId);
                if (entries != null && entries.contains(dialogId)) return true;
            }
            for (String tagId : catalog.getQuickActionIds()) {
                List<String> entries = TAGS.get(tagId);
                if (entries != null && entries.contains(dialogId)) return true;
            }
            return false;
        }

        private List<String> resolve(DialogMenuCatalog.Kind kind) {
            return resolveMenuTagIds(
                kind == DialogMenuCatalog.Kind.PAUSE_SCREEN ? catalog.getPauseScreenIds()
                    : catalog.getQuickActionIds());
        }

    }
}
