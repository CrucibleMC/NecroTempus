package io.github.cruciblemc.necrotempus.api.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

import org.junit.jupiter.api.Test;

class DialogTest {

    @Test
    void noticeWithoutAnActionGetsTheVanillaOkActionAndUsesItForEscape() {
        Dialog dialog = Dialog.notice(new ChatComponentText("Notice"))
            .build();

        assertEquals(
            1,
            dialog.getActions()
                .size());
        assertTrue(
            dialog.getActions()
                .get(0)
                .getLabel() instanceof ChatComponentTranslation);
        assertEquals(
            Dialog.ActionType.NONE,
            dialog.getActions()
                .get(0)
                .getAction()
                .getType());
        assertEquals(0, dialog.getCancelActionIndex());
        assertEquals(
            Dialog.ActionType.NONE,
            dialog.getCancelAction()
                .getAction()
                .getType());
    }

    @Test
    void confirmationUsesItsNoActionForEscape() {
        Dialog dialog = Dialog.confirmation(new ChatComponentText("Confirm"))
            .action(new Dialog.ActionButton(new ChatComponentText("Yes"), Dialog.Action.none()))
            .action(new Dialog.ActionButton(new ChatComponentText("No"), Dialog.Action.custom("test:no", null)))
            .build();

        assertEquals(1, dialog.getCancelActionIndex());
        assertEquals(
            "test:no",
            dialog.getCancelAction()
                .getAction()
                .getValue());
    }

    @Test
    void multiActionWithoutAnExitActionHasNoImplicitCancelAction() {
        Dialog dialog = Dialog.multiAction(new ChatComponentText("Actions"))
            .action(new Dialog.ActionButton(new ChatComponentText("Run"), Dialog.Action.none()))
            .build();

        assertEquals(-1, dialog.getCancelActionIndex());
        assertNull(dialog.getCancelAction());
    }

    @Test
    void noneActionHasNoImplicitUseLimit() {
        assertEquals(
            Integer.MAX_VALUE,
            Dialog.Action.none()
                .getMaxUses());
    }

    @Test
    void serverLinksCanDefineAnExitAction() {
        Dialog dialog = Dialog.serverLinks(new ChatComponentText("Links"))
            .serverLink(new Dialog.ServerLink(new ChatComponentText("Home"), "https://example.com/"))
            .exitAction(new Dialog.ActionButton(new ChatComponentText("Close"), Dialog.Action.none()))
            .build();

        assertEquals(1, dialog.getCancelActionIndex());
        assertEquals(
            Dialog.ActionType.NONE,
            dialog.getCancelAction()
                .getAction()
                .getType());
    }

    @Test
    void vanillaWidthsAndColumnsAreTheDefaults() {
        Dialog dialog = Dialog.multiAction(new ChatComponentText("Layout"))
            .body(new Dialog.TextBody(new ChatComponentText("Body")))
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "", 32, 1))
            .action(new Dialog.ActionButton(new ChatComponentText("OK"), Dialog.Action.none()))
            .build();

        assertEquals(2, dialog.getColumns());
        assertEquals(
            200,
            ((Dialog.TextBody) dialog.getBody()
                .get(0)).getWidth());
        assertEquals(
            200,
            ((Dialog.TextInput) dialog.getInputs()
                .get(0)).getWidth());
        assertEquals(
            150,
            dialog.getActions()
                .get(0)
                .getWidth());
        assertTrue(
            ((Dialog.TextInput) dialog.getInputs()
                .get(0)).isLabelVisible());
    }

    @Test
    void rejectsDimensionsOutsideVanillaSchemaRanges() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Dialog.TextBody(new ChatComponentText("Too wide"), 1025));
        assertThrows(
            IllegalArgumentException.class,
            () -> new Dialog.ActionButton(new ChatComponentText("Too narrow"), Dialog.Action.none(), null, 0));
    }

    @Test
    void inputConstructorsExposeVanillaInitialDefaults() {
        Dialog.TextInput text = new Dialog.TextInput("text", new ChatComponentText("Text"));
        Dialog.NumberRangeInput range = new Dialog.NumberRangeInput(
            "range",
            new ChatComponentText("Range"),
            0,
            10,
            null);
        Dialog.SingleOptionInput option = new Dialog.SingleOptionInput(
            "option",
            new ChatComponentText("Option"),
            java.util.Arrays.asList(new Dialog.Option("first"), new Dialog.Option("second")),
            null);

        assertEquals("", text.getInitial());
        assertEquals(32, text.getMaxLength());
        assertEquals(5.0f, range.getInitial());
        assertEquals("first", option.getInitial());
    }
}
