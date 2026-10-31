package io.github.cruciblemc.necrotempus.api.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.Test;

class DialogCommandTemplateTest {

    @Test
    void expandsTypedValuesAndEscapesTextAsAnSnbtStringFragment() {
        Dialog dialog = Dialog.notice(new ChatComponentText("Template"))
            .input(new Dialog.TextInput("name", new ChatComponentText("Name"), "", 64, 1))
            .input(
                new Dialog.BooleanInput(
                    "enabled",
                    new ChatComponentText("Enabled"),
                    false,
                    new ChatComponentText("active"),
                    new ChatComponentText("inactive")))
            .input(
                new Dialog.SingleOptionInput(
                    "role",
                    new ChatComponentText("Role"),
                    java.util.Arrays.asList(
                        new Dialog.Option("builder", new ChatComponentText("Builder")),
                        new Dialog.Option("tester", new ChatComponentText("Tester"))),
                    "builder"))
            .input(new Dialog.NumberRangeInput("amount", new ChatComponentText("Amount"), 0, 10, null, 1))
            .action(
                new Dialog.ActionButton(
                    new ChatComponentText("Run"),
                    Dialog.Action.dynamicCommand("say \"$(name)\" $(enabled) $(role) $(amount) $(missing)")))
            .build();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "Ada \"Lovelace\" O'Connor\\dev\nnext");
        values.put("enabled", true);
        values.put("role", "tester");
        values.put("amount", 4.5f);

        String command = dialog.getActions()
            .get(0)
            .getAction()
            .resolveCommand(dialog, values);

        assertEquals("say \"Ada \\\"Lovelace\\\" O\\'Connor\\\\dev\\nnext\" active tester 4.5 ", command);
    }

    @Test
    void staticCommandRemainsUnexpanded() {
        Dialog dialog = Dialog.notice(new ChatComponentText("Command"))
            .action(new Dialog.ActionButton(new ChatComponentText("Run"), Dialog.Action.runCommand("say hello")))
            .build();
        assertEquals(
            "say hello",
            dialog.getActions()
                .get(0)
                .getAction()
                .resolveCommand(dialog, null));
    }

    @Test
    void duplicateInputKeyUsesItsLastGetterInCommandTemplates() {
        Dialog dialog = Dialog.notice(new ChatComponentText("Repeated key"))
            .input(new Dialog.TextInput("value", new ChatComponentText("Text"), "", 32, 1))
            .input(
                new Dialog.BooleanInput(
                    "value",
                    new ChatComponentText("Boolean"),
                    false,
                    new ChatComponentText("enabled"),
                    new ChatComponentText("disabled")))
            .action(new Dialog.ActionButton(new ChatComponentText("Run"), Dialog.Action.dynamicCommand("say $(value)")))
            .build();

        assertEquals(
            "say enabled",
            dialog.getActions()
                .get(0)
                .getAction()
                .resolveCommand(dialog, java.util.Collections.singletonMap("value", true)));
    }

    @Test
    void dynamicCommandRequiresAValidMacroAndRejectsLineInjection() {
        assertThrows(IllegalArgumentException.class, () -> Dialog.Action.dynamicCommand("say hello"));
        assertThrows(IllegalArgumentException.class, () -> Dialog.Action.dynamicCommand("say $(bad-key)"));
        assertThrows(IllegalArgumentException.class, () -> Dialog.Action.runCommand("say hello\n/op"));
    }
}
