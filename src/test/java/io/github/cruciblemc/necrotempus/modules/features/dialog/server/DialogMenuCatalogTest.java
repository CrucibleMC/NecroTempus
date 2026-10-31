package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogManager;
import io.github.cruciblemc.necrotempus.api.dialog.DialogMenuCatalog;
import io.github.cruciblemc.necrotempus.modules.features.dialog.compat.crafttweaker.Dialogs;

class DialogMenuCatalogTest {

    @Test
    void catalogCopiesIdsAndSeparatesMenuKinds() {
        List<String> pause = new ArrayList<>(Collections.singletonList("necrotempus:help"));
        DialogMenuCatalog catalog = new DialogMenuCatalog(3, pause, Collections.singletonList("necrotempus:warp"));
        pause.clear();

        assertEquals(3, catalog.getRevision());
        assertEquals(Collections.singletonList("necrotempus:help"), catalog.getPauseScreenIds());
        assertEquals(Collections.singletonList("necrotempus:warp"), catalog.getQuickActionIds());
        assertThrows(
            UnsupportedOperationException.class,
            () -> catalog.getPauseScreenIds()
                .clear());
    }

    @Test
    void catalogRejectsInvalidOrDuplicateIdsAndOversizedMenus() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new DialogMenuCatalog(1, Collections.singletonList("help"), Collections.emptyList()));
        assertThrows(
            IllegalArgumentException.class,
            () -> new DialogMenuCatalog(
                1,
                Arrays.asList("necrotempus:help", "necrotempus:help"),
                Collections.emptyList()));
        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i <= DialogMenuCatalog.MAX_MENU_ENTRIES; i++) tooMany.add("test:dialog_" + i);
        assertThrows(IllegalArgumentException.class, () -> new DialogMenuCatalog(1, tooMany, Collections.emptyList()));
    }

    @Test
    void catalogRequiresPositiveRevision() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new DialogMenuCatalog(0, Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    void menuUsesExternalTitlesAndDropsUnregisteredTargets() {
        String firstId = id();
        String secondId = id();
        String removedId = id();
        Dialog first = notice("Internal title").externalTitle(new ChatComponentText("External title"))
            .build();
        Dialog second = notice("Second title").build();
        DialogManager.register(firstId, first);
        DialogManager.register(secondId, second);
        try {
            Dialog menu = DialogMenuService.resolveMenu(Arrays.asList(firstId, removedId, secondId), "Pause dialogs");

            assertEquals(Dialog.Type.DIALOG_LIST, menu.getType());
            assertEquals(
                "External title",
                menu.getEntries()
                    .get(0)
                    .getLabel()
                    .getUnformattedText());
            assertEquals(
                "Second title",
                menu.getEntries()
                    .get(1)
                    .getLabel()
                    .getUnformattedText());
            assertEquals(
                2,
                menu.getEntries()
                    .size());
        } finally {
            DialogManager.unregister(firstId);
            DialogManager.unregister(secondId);
        }
    }

    @Test
    void oneAvailableMenuEntryOpensItsDialogDirectly() {
        String firstId = id();
        Dialog first = notice("Direct").build();
        DialogManager.register(firstId, first);
        try {
            assertSame(first, DialogMenuService.resolveMenu(Collections.singletonList(firstId), "Pause dialogs"));
            assertNull(DialogMenuService.resolveMenu(Collections.emptyList(), "Pause dialogs"));
        } finally {
            DialogManager.unregister(firstId);
        }
    }

    @Test
    void pauseButtonUsesTheSingleDialogsExternalTitle() {
        String id = id();
        Dialog dialog = notice("Internal").externalTitle(new ChatComponentText("External"))
            .build();
        DialogManager.register(id, dialog);
        try {
            assertEquals(
                "External",
                DialogMenuService.pauseLabel(Collections.singletonList(id), Collections.emptyList())
                    .getUnformattedText());
        } finally {
            DialogManager.unregister(id);
        }
    }

    @Test
    void registeredTagsExpandInOrderAndDeduplicateAcrossTags() {
        String firstTag = id();
        String secondTag = id();
        String firstDialog = id();
        String sharedDialog = id();
        String lastDialog = id();
        DialogManager.registerTag(firstTag, Arrays.asList(firstDialog, sharedDialog));
        DialogManager.registerTag(secondTag, Arrays.asList(sharedDialog, lastDialog));
        try {
            assertEquals(
                Arrays.asList(firstDialog, sharedDialog, lastDialog),
                DialogMenuService.expandTags(Arrays.asList(firstTag, secondTag)));
            assertThrows(
                UnsupportedOperationException.class,
                () -> DialogMenuService.expandTags(Collections.singletonList(firstTag))
                    .clear());
        } finally {
            DialogManager.unregisterTag(firstTag);
            DialogManager.unregisterTag(secondTag);
        }
    }

    @Test
    void menuTagResolutionTracksTagAndDialogChangesAfterConfiguration() {
        String tag = id();
        String firstId = id();
        String secondId = id();
        DialogManager.registerTag(tag, Collections.singletonList(firstId));
        try {
            assertEquals(Collections.emptyList(), DialogMenuService.resolveMenuTagIds(Collections.singletonList(tag)));
            DialogManager.register(firstId, notice("First").build());
            assertEquals(
                Collections.singletonList(firstId),
                DialogMenuService.resolveMenuTagIds(Collections.singletonList(tag)));

            DialogManager.registerTag(tag, Collections.singletonList(secondId));
            DialogManager.register(secondId, notice("Second").build());
            assertEquals(
                Collections.singletonList(secondId),
                DialogMenuService.resolveMenuTagIds(Collections.singletonList(tag)));

            DialogManager.unregisterTag(tag);
            assertEquals(Collections.emptyList(), DialogMenuService.resolveMenuTagIds(Collections.singletonList(tag)));
        } finally {
            DialogManager.unregisterTag(tag);
            DialogManager.unregister(firstId);
            DialogManager.unregister(secondId);
        }
    }

    @Test
    void tagsRequireNamespacedUniqueIdsAndUnknownTagsFailLoudly() {
        String tag = id();
        String dialog = id();
        assertThrows(
            IllegalArgumentException.class,
            () -> DialogManager.registerTag("invalid", Collections.emptyList()));
        assertThrows(
            IllegalArgumentException.class,
            () -> DialogManager.registerTag(tag, Arrays.asList(dialog, dialog)));
        DialogManager.registerTag(tag, Collections.singletonList(dialog));
        try {
            assertThrows(
                IllegalArgumentException.class,
                () -> DialogMenuService.expandTags(Collections.singletonList(id())));
        } finally {
            DialogManager.unregisterTag(tag);
        }
    }

    @Test
    void craftTweakerTagUndoRestoresPreviousDefinition() throws Exception {
        String tagId = id();
        String firstDialog = id();
        String replacementDialog = id();
        Class<?> actionClass = Class.forName(Dialogs.class.getName() + "$RegisterTagAction");
        Constructor<?> constructor = actionClass.getConstructor(String.class, List.class);
        Object first = constructor.newInstance(tagId, Collections.singletonList(firstDialog));
        Object replacement = constructor.newInstance(tagId, Collections.singletonList(replacementDialog));
        Method apply = actionClass.getMethod("apply");
        Method undo = actionClass.getMethod("undo");

        apply.invoke(first);
        apply.invoke(replacement);
        assertEquals(
            Collections.singletonList(replacementDialog),
            DialogMenuService.expandTags(Collections.singletonList(tagId)));
        undo.invoke(replacement);
        assertEquals(
            Collections.singletonList(firstDialog),
            DialogMenuService.expandTags(Collections.singletonList(tagId)));
        undo.invoke(first);
        assertThrows(
            IllegalArgumentException.class,
            () -> DialogMenuService.expandTags(Collections.singletonList(tagId)));
    }

    @Test
    void serverLinksKeepOrderAndDuplicateLabels() {
        List<Dialog.ServerLink> links = Arrays.asList(
            new Dialog.ServerLink(new ChatComponentText("Community"), "https://example.com/one"),
            new Dialog.ServerLink(new ChatComponentText("Community"), "https://example.com/two"));

        Dialog dialog = DialogMenuService.resolveServerLinks(links, "Links");

        assertEquals(Dialog.Type.SERVER_LINKS, dialog.getType());
        assertEquals(
            2,
            dialog.getServerLinks()
                .size());
        assertEquals(
            "Community",
            dialog.getServerLinks()
                .get(0)
                .getLabel()
                .getUnformattedText());
        assertEquals(
            "Community",
            dialog.getServerLinks()
                .get(1)
                .getLabel()
                .getUnformattedText());
        assertEquals(
            "https://example.com/one",
            dialog.getServerLinks()
                .get(0)
                .getUrl());
        assertEquals(
            "https://example.com/two",
            dialog.getServerLinks()
                .get(1)
                .getUrl());
    }

    private static Dialog.Builder notice(String title) {
        return Dialog.notice(new ChatComponentText(title))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()));
    }

    private static String id() {
        return "test:" + UUID.randomUUID()
            .toString()
            .replace("-", "");
    }
}
