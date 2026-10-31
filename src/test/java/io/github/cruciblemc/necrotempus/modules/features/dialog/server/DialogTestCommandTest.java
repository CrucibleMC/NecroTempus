package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.api.dialog.DialogResponse;

class DialogTestCommandTest {

    @Test
    void providesScriptFreeCasesForEveryImplementedDialogType() {
        assertEquals(
            Dialog.Type.NOTICE,
            DialogTestCommand.createTestDialog("notice")
                .getType());
        assertEquals(
            Dialog.Type.CONFIRMATION,
            DialogTestCommand.createTestDialog("confirmation")
                .getType());
        Dialog multi = DialogTestCommand.createTestDialog("multi");
        assertEquals(Dialog.Type.MULTI_ACTION, multi.getType());
        assertEquals(
            5,
            multi.getActions()
                .size());
        Dialog form = DialogTestCommand.createTestDialog("form");
        assertEquals(
            4,
            form.getInputs()
                .size());
        assertEquals(
            "name",
            form.getInputs()
                .get(0)
                .getKey());
        assertEquals(
            "necrotempus:test_custom",
            DialogTestCommand.createTestDialog("custom")
                .getActions()
                .get(0)
                .getAction()
                .getValue());
    }

    @Test
    void rejectsUnknownTestCases() {
        assertThrows(IllegalArgumentException.class, () -> DialogTestCommand.createTestDialog("unknown"));
    }

    @Test
    void reportsCustomActionIdsInTheIntegrationCommand() {
        DialogResponse response = new DialogResponse(
            0,
            false,
            java.util.Collections.<String, Object>emptyMap(),
            Dialog.Action.custom("necrotempus:ct_custom", null));

        assertTrue(
            DialogTestCommand.formatResponse("test:dialog", response)
                .contains("necrotempus:ct_custom"));
    }
}
