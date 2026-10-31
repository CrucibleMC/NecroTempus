package io.github.cruciblemc.necrotempus.api.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import org.junit.jupiter.api.Test;

class DialogResponseTest {

    @Test
    void staticCustomActionsPreserveOptionalPayloadTagsAndIgnoreInputs() {
        NBTTagList payload = new NBTTagList();
        payload.appendTag(new NBTTagString("value"));
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "ignored");

        for (NBTBase original : new NBTBase[] { null, new NBTTagString("scalar"), payload }) {
            DialogResponse response = new DialogResponse(
                0,
                false,
                values,
                Dialog.Action.custom("test:static", original));
            assertEquals("test:static", response.getActionId());
            assertEquals(original, response.getActionPayload());
            assertTrue(
                response.getInputValues()
                    .isEmpty());
        }
    }

    @Test
    void dynamicCustomActionsCopyAdditionsAndLetInputsOverrideThemWithoutReplacingActionId() {
        NBTTagCompound additions = new NBTTagCompound();
        additions.setInteger("count", 1);
        additions.setString("unchanged", "static");
        Dialog.Action action = Dialog.Action.dynamicCustom("test:submit", additions);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("count", "from-input");
        values.put("id", "not-the-action-id");

        DialogResponse response = new DialogResponse(0, false, values, action);
        NBTTagCompound merged = (NBTTagCompound) response.getActionPayload();

        assertEquals("test:submit", response.getActionId());
        assertEquals(new NBTTagString("not-the-action-id"), merged.getTag("id"));
        assertEquals(new NBTTagString("from-input"), merged.getTag("count"));
        assertEquals(new NBTTagString("static"), merged.getTag("unchanged"));
        assertEquals(1, additions.getInteger("count"));
        assertNull(response.getText("missing"));
    }
}
