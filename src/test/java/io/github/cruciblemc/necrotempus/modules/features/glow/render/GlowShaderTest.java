package io.github.cruciblemc.necrotempus.modules.features.glow.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class GlowShaderTest {

    @Test
    void outlineUsesTheColorStoredInTheGlowMask() throws IOException {
        String source = new String(
            Files.readAllBytes(Paths.get("src/main/resources/assets/necrotempus/shaders/program/glow_outline.fsh")),
            StandardCharsets.UTF_8);
        assertTrue(source.contains("s.rgb"));
    }
}
