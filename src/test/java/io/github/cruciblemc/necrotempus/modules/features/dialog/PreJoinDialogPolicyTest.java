package io.github.cruciblemc.necrotempus.modules.features.dialog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class PreJoinDialogPolicyTest {

    @Test
    void acceptsSupportedTypesActionsAndSingleLineForms() {
        Dialog[] dialogs = { Dialog.notice(text("Notice"))
            .action(button("OK", Dialog.Action.none()))
            .build(),
            Dialog.confirmation(text("Confirm"))
                .action(button("Yes", Dialog.Action.none()))
                .action(button("No", Dialog.Action.none()))
                .build(),
            Dialog.multiAction(text("Choose"))
                .action(button("Pick", Dialog.Action.none()))
                .build() };
        for (Dialog original : dialogs) {
            Dialog.Builder builder = original.getType() == Dialog.Type.NOTICE ? Dialog.notice(text("Notice"))
                : original.getType() == Dialog.Type.CONFIRMATION ? Dialog.confirmation(text("Confirm"))
                    : Dialog.multiAction(text("Choose"));
            builder.afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .body(new Dialog.TextBody(text("One line")))
                .input(new Dialog.TextInput("name", text("Name"), "Ada", 32, 1))
                .input(new Dialog.BooleanInput("enabled", text("Enabled"), true, text("Yes"), text("No")))
                .input(
                    new Dialog.SingleOptionInput(
                        "color",
                        text("Color"),
                        Arrays.asList(new Dialog.Option("red"), new Dialog.Option("blue")),
                        "red"))
                .input(new Dialog.NumberRangeInput("amount", text("Amount"), 0, 10, 1.0f, 5));
            for (Dialog.ActionButton action : original.getActions()) builder.action(action);
            Dialog dialog = builder.build();
            PreJoinDialogPolicy.validate(dialog);
        }

        Dialog custom = Dialog.notice(text("Custom"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .action(button("CUSTOM", Dialog.Action.dynamicCustom("form", new net.minecraft.nbt.NBTTagCompound())))
            .build();
        assertDoesNotThrow(() -> PreJoinDialogPolicy.validate(custom));
    }

    @Test
    void acceptsDynamicCustomSanitizedByDialogCodec() {
        Dialog source = Dialog.notice(text("Form"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
            .action(
                button("Submit", Dialog.Action.dynamicCustom("private:submit", new net.minecraft.nbt.NBTTagCompound())))
            .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            DialogCodec.write(buffer, source);
            Dialog decoded = DialogCodec.read(buffer);
            Dialog.Action sanitized = decoded.getActions()
                .get(0)
                .getAction();

            assertEquals(Dialog.ActionType.CUSTOM, sanitized.getType());
            assertTrue(sanitized.isDynamic());
            assertEquals("necrotempus:server_action", sanitized.getValue());
            assertEquals(new net.minecraft.nbt.NBTTagCompound(), sanitized.getPayload());
            assertDoesNotThrow(() -> PreJoinDialogPolicy.validate(decoded));
        } finally {
            buffer.release();
        }
    }

    @Test
    void limitsOnlyDynamicPreJoinFormsToResponseCodecCapacity() {
        Dialog.Builder boundary = formWithBooleanInputs(64)
            .action(button("Submit", Dialog.Action.dynamicCustom("submit", new net.minecraft.nbt.NBTTagCompound())));
        assertDoesNotThrow(() -> PreJoinDialogPolicy.validate(boundary.build()));

        Dialog overBoundary = formWithBooleanInputs(65)
            .action(button("Submit", Dialog.Action.dynamicCustom("submit", new net.minecraft.nbt.NBTTagCompound())))
            .build();
        assertRejected(overBoundary);

        Dialog exitActionOverBoundary = formWithBooleanInputs(65)
            .exitAction(button("Submit", Dialog.Action.dynamicCustom("submit", new net.minecraft.nbt.NBTTagCompound())))
            .build();
        assertRejected(exitActionOverBoundary);

        Dialog staticOnlyOverBoundary = formWithBooleanInputs(65).action(button("Close", Dialog.Action.none()))
            .build();
        assertDoesNotThrow(() -> PreJoinDialogPolicy.validate(staticOnlyOverBoundary));
    }

    private static Dialog.Builder formWithBooleanInputs(int count) {
        Dialog.Builder builder = Dialog.notice(text("Form"))
            .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE);
        for (int i = 0; i < count; i++) {
            builder.input(new Dialog.BooleanInput("input" + i, text("Input"), false, null, null));
        }
        return builder;
    }

    @Test
    void rejectsUnsupportedTypesAndAfterActions() {
        assertRejected(
            Dialog.notice(text("close"))
                .afterAction(Dialog.AfterAction.CLOSE)
                .build());
        assertRejected(
            Dialog.notice(text("none"))
                .pause(false)
                .afterAction(Dialog.AfterAction.NONE)
                .build());
        assertRejected(
            Dialog.dialogList(text("list"))
                .entryTag("minecraft:stone")
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .build());
        assertRejected(
            Dialog.serverLinks(text("links"))
                .serverLink(new Dialog.ServerLink(text("link"), "https://example.com"))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .build());
    }

    @Test
    void rejectsItemsMultilineTextAndMultilineInputs() {
        assertRejected(
            Dialog.notice(text("item"))
                .body(
                    new Dialog.ItemBody(
                        new net.minecraft.item.ItemStack(new net.minecraft.item.Item(), 1, 0),
                        text("description")))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .build());
        assertRejected(
            Dialog.notice(text("title\nline"))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .build());
        assertRejected(
            Dialog.notice(text("body"))
                .body(new Dialog.TextBody(text("first\nsecond")))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .build());
        assertRejected(
            Dialog.notice(text("input"))
                .input(new Dialog.TextInput("name", text("Name"), "", 32, 2))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .build());
    }

    @Test
    void rejectsNonCustomActionsAndDynamicNonCustomActions() {
        for (Dialog.Action action : Arrays.asList(
            Dialog.Action.runCommand("say hi"),
            Dialog.Action.openUrl("https://example.com"),
            Dialog.Action.suggestCommand("/help"),
            Dialog.Action.copyToClipboard("secret"),
            Dialog.Action.showDialog("example:next"))) {
            assertRejected(
                Dialog.notice(text("action"))
                    .action(button("bad", action))
                    .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                    .build());
        }
    }

    @Test
    void rejectsBodyClickEventsAndExplicitActionSpans() {
        ChatComponentText clicked = text("click");
        clicked.getChatStyle()
            .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com"));
        assertRejected(
            Dialog.notice(text("click"))
                .body(new Dialog.TextBody(clicked))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .build());

        assertRejected(
            Dialog.notice(text("span"))
                .body(
                    new Dialog.TextBody(
                        text("click"),
                        200,
                        Arrays.asList(new Dialog.ActionSpan(0, 5, Dialog.Action.runCommand("say hi")))))
                .afterAction(Dialog.AfterAction.WAIT_FOR_RESPONSE)
                .build());
    }

    private static void assertRejected(Dialog dialog) {
        assertThrows(IllegalArgumentException.class, () -> PreJoinDialogPolicy.validate(dialog));
    }

    private static Dialog.ActionButton button(String label, Dialog.Action action) {
        return new Dialog.ActionButton(text(label), action);
    }

    private static ChatComponentText text(String value) {
        return new ChatComponentText(value);
    }
}
