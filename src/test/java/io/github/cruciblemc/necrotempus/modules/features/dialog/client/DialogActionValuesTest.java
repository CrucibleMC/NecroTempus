package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;
import io.github.cruciblemc.necrotempus.modules.features.dialog.DialogFormValues;

class DialogActionValuesTest {

    @Test
    void onlyDynamicActionsSubmitFormValues() {
        Map<String, Object> values = Collections.singletonMap("name", "Ada");
        assertEquals(Collections.emptyMap(), DialogFormValues.forAction(Dialog.Action.none(), values));
        assertEquals(Collections.emptyMap(), DialogFormValues.forAction(null, values));
        assertEquals(Collections.emptyMap(), DialogFormValues.forAction(Dialog.Action.runCommand("help"), values));
        assertEquals(values, DialogFormValues.forAction(Dialog.Action.dynamicCommand("help $(name)"), values));
    }
}
