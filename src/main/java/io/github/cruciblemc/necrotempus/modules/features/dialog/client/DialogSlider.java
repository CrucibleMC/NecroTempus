package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;

final class DialogSlider {

    private DialogSlider() {}

    static float valueAt(Dialog.NumberRangeInput input, float t) {
        t = Math.max(0, Math.min(1, t));
        float start = input.getStart();
        float end = input.getEnd();
        Float step = input.getStep();
        float value = (float) ((double) start + ((double) end - start) * t);
        if (step == null) return value;
        float initial = input.getInitial();
        int selectedStep = Math.round((value - initial) / step);
        float stepped = initial + selectedStep * step;
        if (stepped < Math.min(start, end) || stepped > Math.max(start, end)) {
            stepped = initial + (selectedStep - Integer.signum(selectedStep)) * step;
        }
        return stepped;
    }

    static String valueToString(float value) {
        int integer = (int) value;
        return (float) integer == value ? Integer.toString(integer) : Float.toString(value);
    }
}
