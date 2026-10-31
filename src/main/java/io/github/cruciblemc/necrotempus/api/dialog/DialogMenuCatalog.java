package io.github.cruciblemc.necrotempus.api.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DialogMenuCatalog {

    public static final int MAX_MENU_ENTRIES = 255;
    public static final int MAX_SERVER_LINKS = 255;

    public enum Kind {
        PAUSE_SCREEN,
        QUICK_ACTION,
        SERVER_LINKS
    }

    private final int revision;
    private final List<String> pauseScreenIds;
    private final List<String> quickActionIds;
    private final List<Dialog.ServerLink> serverLinks;

    public DialogMenuCatalog(int revision, List<String> pauseScreenIds, List<String> quickActionIds) {
        this(revision, pauseScreenIds, quickActionIds, Collections.emptyList());
    }

    public DialogMenuCatalog(int revision, List<String> pauseScreenIds, List<String> quickActionIds,
        List<Dialog.ServerLink> serverLinks) {
        if (revision < 1) throw new IllegalArgumentException("Menu catalog revision must be positive");
        this.revision = revision;
        this.pauseScreenIds = copyIds(pauseScreenIds);
        this.quickActionIds = copyIds(quickActionIds);
        if (serverLinks == null || serverLinks.size() > MAX_SERVER_LINKS) {
            throw new IllegalArgumentException("Invalid server links");
        }
        this.serverLinks = Collections.unmodifiableList(new ArrayList<>(serverLinks));
    }

    public int getRevision() {
        return revision;
    }

    public List<String> getPauseScreenIds() {
        return pauseScreenIds;
    }

    public List<String> getQuickActionIds() {
        return quickActionIds;
    }

    public List<Dialog.ServerLink> getServerLinks() {
        return serverLinks;
    }

    public List<String> getIds(Kind kind) {
        if (kind == null) throw new IllegalArgumentException("Menu kind is required");
        if (kind == Kind.PAUSE_SCREEN) return pauseScreenIds;
        if (kind == Kind.QUICK_ACTION) return quickActionIds;
        throw new IllegalArgumentException("Server links are not dialog IDs");
    }

    private static List<String> copyIds(List<String> ids) {
        if (ids == null || ids.size() > MAX_MENU_ENTRIES) {
            throw new IllegalArgumentException("Invalid dialog menu entries");
        }
        List<String> copy = new ArrayList<>(ids.size());
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            DialogManager.requireId(id);
            if (!seen.add(id)) throw new IllegalArgumentException("Duplicate dialog menu ID: " + id);
            copy.add(id);
        }
        return Collections.unmodifiableList(copy);
    }
}
