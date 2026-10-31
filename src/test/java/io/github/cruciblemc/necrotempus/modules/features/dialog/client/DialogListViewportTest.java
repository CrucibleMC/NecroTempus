package io.github.cruciblemc.necrotempus.modules.features.dialog.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DialogListViewportTest {

    @Test
    void pixelScrollOffsetStaysInsideTheContentViewport() {
        assertEquals(0, DialogListViewport.clampPixelOffset(-1, 900, 300));
        assertEquals(240, DialogListViewport.clampPixelOffset(240, 900, 300));
        assertEquals(600, DialogListViewport.clampPixelOffset(900, 900, 300));
        assertEquals(0, DialogListViewport.clampPixelOffset(40, 200, 300));
    }

    @Test
    void contentHitboxesMustStayInsideTheVisibleViewport() {
        assertEquals(true, DialogListViewport.contains(100, 100, 200));
        assertEquals(false, DialogListViewport.contains(300, 100, 200));
        assertEquals(true, DialogListViewport.fullyVisible(120, 20, 100, 200));
        assertEquals(false, DialogListViewport.fullyVisible(290, 20, 100, 200));
    }
}
