package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import net.minecraft.event.ClickEvent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogBodyActions;
import io.github.cruciblemc.necrotempus.modules.features.dialog.DialogCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class DialogBodyActionTest {

    @Test
    void textBodyClickEventUsesAuthorizedActionIndexAfterButtons() {
        ChatComponentText link = new ChatComponentText("Open website");
        link.setChatStyle(
            new ChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com/")));
        Dialog dialog = Dialog.notice(new ChatComponentText("Notice"))
            .body(new Dialog.TextBody(link))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();

        Dialog.Action action = DialogSessions.actionFor(dialog, 1);

        assertNotNull(action, "the body event must be part of the server-side action table");
        assertEquals(Dialog.ActionType.OPEN_URL, action.getType());
        assertEquals("https://example.com/", action.getValue());
    }

    @Test
    void itemDescriptionClickEventFollowsTextBodyAction() {
        ChatComponentText textLink = new ChatComponentText("Read docs");
        textLink.setChatStyle(
            new ChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://docs.example/")));
        ChatComponentText itemLink = new ChatComponentText("Item details");
        itemLink.setChatStyle(
            new ChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://item.example/")));
        Dialog dialog = Dialog.notice(new ChatComponentText("Links"))
            .body(new Dialog.TextBody(textLink))
            .body(new Dialog.ItemBody(new ItemStack(new Item(), 3, 0), itemLink))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();

        Dialog.Action action = DialogSessions.actionFor(dialog, 2);

        assertNotNull(action);
        assertEquals(Dialog.ActionType.OPEN_URL, action.getType());
        assertEquals("https://item.example/", action.getValue());
    }

    @Test
    void bodyActionSpanTracksClickableTextWithinSiblings() {
        IChatComponent text = new ChatComponentText("Read ");
        ChatComponentText link = new ChatComponentText("docs");
        link.setChatStyle(
            new ChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com/")));
        text.appendSibling(link)
            .appendText(" now");

        List<DialogBodyActions.Span> spans = DialogBodyActions.spans(text);

        assertEquals(1, spans.size());
        assertEquals(
            5,
            spans.get(0)
                .getStart());
        assertEquals(
            9,
            spans.get(0)
                .getEnd());
        assertEquals(
            0,
            spans.get(0)
                .getActionOrdinal());
        assertEquals(
            Dialog.ActionType.OPEN_URL,
            spans.get(0)
                .getAction()
                .getType());
    }

    @Test
    void explicitBodySpansRepresentModernActionsAndRoundTrip() {
        Dialog.TextBody text = new Dialog.TextBody(
            new ChatComponentText("copy next"),
            200,
            Arrays.asList(
                new Dialog.ActionSpan(0, 4, Dialog.Action.copyToClipboard("copied")),
                new Dialog.ActionSpan(5, 9, Dialog.Action.showDialog("test:next"))));
        Dialog source = Dialog.notice(new ChatComponentText("Actions"))
            .body(text)
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();

        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, source);
            Dialog decoded = DialogCodec.read(buffer);
            List<Dialog.Action> actions = DialogBodyActions.collect(decoded);

            assertEquals(2, actions.size());
            assertEquals(
                Dialog.ActionType.COPY_TO_CLIPBOARD,
                actions.get(0)
                    .getType());
            assertEquals(
                Dialog.ActionType.SHOW_DIALOG,
                actions.get(1)
                    .getType());
            assertNotEquals(
                "test:next",
                actions.get(1)
                    .getValue());
            assertEquals(
                Dialog.ActionType.COPY_TO_CLIPBOARD,
                DialogSessions.actionFor(decoded, 1)
                    .getType());
            assertEquals(
                Dialog.ActionType.SHOW_DIALOG,
                DialogSessions.actionFor(decoded, 2)
                    .getType());
        } finally {
            buffer.release();
        }
    }

    @Test
    void itemDescriptionSupportsExplicitCustomActionSpans() {
        Dialog.ItemBody item = new Dialog.ItemBody(
            new ItemStack(new Item(), 1, 0),
            new ChatComponentText("details"),
            true,
            true,
            16,
            16,
            200,
            java.util.Collections.singletonList(new Dialog.ActionSpan(0, 7, Dialog.Action.custom("test:item", null))));
        Dialog dialog = Dialog.notice(new ChatComponentText("Actions"))
            .body(item)
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();

        assertEquals(
            Dialog.ActionType.CUSTOM,
            DialogBodyActions.collect(dialog)
                .get(0)
                .getType());
        assertEquals(
            Dialog.ActionType.CUSTOM,
            DialogSessions.actionFor(dialog, 1)
                .getType());
    }

    @Test
    void codecRejectsUnsupportedBodyClickEvents() {
        ChatComponentText body = new ChatComponentText("Open local file");
        body.setChatStyle(
            new ChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, "file:///tmp/private")));
        Dialog dialog = Dialog.notice(new ChatComponentText("Links"))
            .body(new Dialog.TextBody(body))
            .action(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            assertThrows(IllegalArgumentException.class, () -> DialogCodec.write(buffer, dialog));
        } finally {
            buffer.release();
        }
    }
}
