package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class DialogLayoutTest {

    @Test
    void centersIncompleteLastRowsForTwoAndThreeColumns() {
        assertEquals(10, DialogLayout.buttonX(0, 322, Arrays.asList(150, 150, 150, 150, 150), 2, 0));
        assertEquals(86, DialogLayout.buttonX(0, 322, Arrays.asList(150, 150, 150, 150, 150), 2, 4));
        assertEquals(76, DialogLayout.buttonX(0, 454, Arrays.asList(150, 150, 150, 150, 150), 3, 3));
    }

    @Test
    void alignsUnequalButtonsToGridColumnsAndCentersTheLastRow() {
        assertEquals(23, DialogLayout.buttonX(0, 500, Arrays.asList(100, 150, 200, 80, 60), 3, 0));
        assertEquals(125, DialogLayout.buttonX(0, 500, Arrays.asList(100, 150, 200, 80, 60), 3, 1));
        assertEquals(179, DialogLayout.buttonX(0, 500, Arrays.asList(100, 150, 200, 80, 60), 3, 3));
        assertEquals(454, DialogLayout.gridWidth(Arrays.asList(100, 150, 200, 80, 60), 3, 2));
    }

    @Test
    void keepsRequiredButtonWidthUnlessTheScreenIsNarrower() {
        assertEquals(322, DialogLayout.panelWidth(500, 298));
        assertEquals(268, DialogLayout.panelWidth(300, 298));
    }

    @Test
    void centersTextInputLabelAndFieldAsOneControl() {
        assertEquals(144, DialogLayout.labeledControlX(20, 400, 40, 8, 200));
        assertEquals(120, DialogLayout.labeledControlX(20, 400, 0, 0, 200));
    }
}
