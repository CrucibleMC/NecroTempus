package io.github.cruciblemc.necrotempus.modules.features.glow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlowingEntityRegistryTest {

    @Test
    void storesAndReturnsColor() {
        GlowingEntityRegistry r = new GlowingEntityRegistry();
        r.set(7, 0xFF5555, 100);
        assertEquals(Integer.valueOf(0xFF5555), r.colorFor(7));
        assertFalse(r.isEmpty());
    }

    @Test
    void colorSentinelPreserved() {
        GlowingEntityRegistry r = new GlowingEntityRegistry();
        r.set(7, -1, 100);
        assertEquals(Integer.valueOf(-1), r.colorFor(7));
    }

    @Test
    void unknownEntityHasNoColor() {
        GlowingEntityRegistry r = new GlowingEntityRegistry();
        assertNull(r.colorFor(42));
    }

    @Test
    void finiteEntryExpires() {
        GlowingEntityRegistry r = new GlowingEntityRegistry();
        r.set(7, 0xFFFFFF, 2);
        r.tick(); // 2 -> 1
        assertEquals(Integer.valueOf(0xFFFFFF), r.colorFor(7));
        r.tick(); // 1 -> 0, pruned
        assertNull(r.colorFor(7));
        assertTrue(r.isEmpty());
    }

    @Test
    void infiniteEntryNeverExpires() {
        GlowingEntityRegistry r = new GlowingEntityRegistry();
        r.set(7, 0xFFFFFF, 0); // <= 0 => infinite
        for (int i = 0; i < 1000; i++) r.tick();
        assertEquals(Integer.valueOf(0xFFFFFF), r.colorFor(7));
    }

    @Test
    void removeAndClear() {
        GlowingEntityRegistry r = new GlowingEntityRegistry();
        r.set(1, 0xFFFFFF, 100);
        r.set(2, 0xFFFFFF, 100);
        r.remove(1);
        assertNull(r.colorFor(1));
        assertEquals(Integer.valueOf(0xFFFFFF), r.colorFor(2));
        r.clear();
        assertTrue(r.isEmpty());
    }

    @Test
    void glowingIdsSnapshotIsStable() {
        GlowingEntityRegistry r = new GlowingEntityRegistry();
        r.set(3, 0xFFFFFF, 100);
        r.set(9, 0xFFFFFF, 100);
        int[] ids = r.glowingIds();
        java.util.Arrays.sort(ids);
        assertArrayEquals(new int[] { 3, 9 }, ids);
    }
}
