package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.ChatComponentText;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.dialog.Dialog;

class DialogSliderTest {

    @Test
    void snapsTheRightEdgeToTheLastStepFromInitial() {
        Dialog.NumberRangeInput input = new Dialog.NumberRangeInput(
            "amount",
            new ChatComponentText("Amount"),
            0,
            10,
            4.0f,
            1.0f);

        assertEquals(9.0f, DialogSlider.valueAt(input, 1.0f));
    }

    @Test
    void steppedRangeKeepsBothEndsReachableAroundItsInitialValue() {
        Dialog.NumberRangeInput input = new Dialog.NumberRangeInput(
            "amount",
            new ChatComponentText("Amount"),
            0,
            10,
            1.0f,
            5.0f);

        assertEquals(0.0f, DialogSlider.valueAt(input, 0.0f));
        assertEquals(5.0f, DialogSlider.valueAt(input, 0.5f));
        assertEquals(10.0f, DialogSlider.valueAt(input, 1.0f));
    }

    @Test
    void interpolatesDescendingAndConstantRanges() {
        Dialog.NumberRangeInput descending = new Dialog.NumberRangeInput(
            "amount",
            new ChatComponentText("Amount"),
            10,
            0,
            null,
            5.0f);
        Dialog.NumberRangeInput constant = new Dialog.NumberRangeInput(
            "amount",
            new ChatComponentText("Amount"),
            5,
            5,
            null,
            5.0f);

        assertEquals(7.5f, DialogSlider.valueAt(descending, 0.25f));
        assertEquals(5.0f, DialogSlider.valueAt(constant, 0.8f));
    }

    @Test
    void formatsWholeAndFractionalValuesLikeVanilla() {
        assertEquals("5", DialogSlider.valueToString(5.0f));
        assertEquals("5.25", DialogSlider.valueToString(5.25f));
    }
}
