package io.github.cruciblemc.necrotempus.modules.features.glow.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class GlowClientManagerTest {

    @Test
    void debugTargetingRaycastsDroppedItems() throws IOException {
        String source = new String(
            Files.readAllBytes(Paths.get(
                "src/main/java/io/github/cruciblemc/necrotempus/modules/features/glow/client/GlowClientManager.java")),
            StandardCharsets.UTF_8);
        assertTrue(source.contains("EntityItem.class"));
        assertTrue(source.contains("calculateIntercept"));
    }

    @Test
    void debugVolumeKeyCreatesABox() throws IOException {
        String source = new String(
            Files.readAllBytes(Paths.get(
                "src/main/java/io/github/cruciblemc/necrotempus/modules/features/glow/client/GlowClientManager.java")),
            StandardCharsets.UTF_8);
        assertTrue(source.contains("Keyboard.KEY_V"));
        assertTrue(source.contains("setVolume"));
        assertTrue(source.contains("new GlowVolume"));
        assertTrue(source.contains("0xFF0000"));
        assertTrue(source.contains(">> 4"));
        assertTrue(source.contains("getHeight()"));
    }
}
