package io.github.cruciblemc.necrotempus.api.dialog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagString;

public final class DialogResponse {

    private final int actionIndex;
    private final boolean cancelled;
    private final Map<String, Object> values;
    private final String actionId;
    private final NBTBase actionPayload;

    public DialogResponse(int actionIndex, boolean cancelled) {
        this(actionIndex, cancelled, Collections.emptyMap());
    }

    public DialogResponse(int actionIndex, boolean cancelled, Map<String, Object> values) {
        this(actionIndex, cancelled, values, null);
    }

    public DialogResponse(int actionIndex, boolean cancelled, Map<String, Object> values, Dialog.Action action) {
        this.actionIndex = actionIndex;
        this.cancelled = cancelled;
        boolean custom = action != null && action.getType() == Dialog.ActionType.CUSTOM;
        actionId = custom ? action.getValue() : null;
        this.values = Collections
            .unmodifiableMap(new LinkedHashMap<>(custom && !action.isDynamic() ? Collections.emptyMap() : values));
        actionPayload = custom ? customPayload(action, this.values) : null;
    }

    public int getActionIndex() {
        return actionIndex;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public String getActionId() {
        return actionId;
    }

    public NBTBase getActionPayload() {
        return actionPayload == null ? null : actionPayload.copy();
    }

    public Map<String, Object> getInputValues() {
        return values;
    }

    public String getText(String key) {
        Object value = values.get(key);
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        throw new IllegalArgumentException("Dialog input is not text: " + key);
    }

    public Boolean getBoolean(String key) {
        Object value = values.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        throw new IllegalArgumentException("Dialog input is not boolean: " + key);
    }

    public Float getFloat(String key) {
        Object value = values.get(key);
        if (value == null) return null;
        if (value instanceof Float) return (Float) value;
        throw new IllegalArgumentException("Dialog input is not a number: " + key);
    }

    private static NBTBase customPayload(Dialog.Action action, Map<String, Object> values) {
        NBTBase payload = action.getPayload();
        if (!action.isDynamic()) return payload;

        NBTTagCompound merged = (NBTTagCompound) payload;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) merged.setTag(entry.getKey(), new NBTTagString((String) value));
            else if (value instanceof Boolean)
                merged.setTag(entry.getKey(), new NBTTagByte((byte) ((Boolean) value ? 1 : 0)));
            else if (value instanceof Float) merged.setTag(entry.getKey(), new NBTTagFloat((Float) value));
            else throw new IllegalArgumentException("Unsupported dynamic custom input: " + entry.getKey());
        }
        return merged;
    }
}
