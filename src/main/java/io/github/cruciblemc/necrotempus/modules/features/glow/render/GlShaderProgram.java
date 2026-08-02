package io.github.cruciblemc.necrotempus.modules.features.glow.render;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/** Compiles and links a vertex+fragment program from resources. Immediate-mode friendly (GLSL 120). */
public class GlShaderProgram {

    private static final Logger LOG = LogManager.getLogger("GlowShaderProgram");

    private int program = 0;
    private boolean valid = false;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public GlShaderProgram(ResourceLocation vsh, ResourceLocation fsh) {
        try {
            int v = compile(GL20.GL_VERTEX_SHADER, read(vsh));
            int f = compile(GL20.GL_FRAGMENT_SHADER, read(fsh));
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, v);
            GL20.glAttachShader(program, f);
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                LOG.error("Glow program link failed: {}", GL20.glGetProgramInfoLog(program, 4096));
                GL20.glDeleteProgram(program);
            } else {
                valid = true;
            }
            GL20.glDeleteShader(v);
            GL20.glDeleteShader(f);
        } catch (Exception e) {
            LOG.error("Failed to build glow shader program", e);
        }
    }

    private static String read(ResourceLocation loc) throws Exception {
        InputStream in = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(loc)
            .getInputStream();
        try (Scanner s = new Scanner(in, StandardCharsets.UTF_8.name())) {
            return s.useDelimiter("\\A")
                .next();
        }
    }

    private static int compile(int type, String src) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, src);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            LOG.error("Glow shader compile failed: {}", GL20.glGetShaderInfoLog(shader, 4096));
        }
        return shader;
    }

    public int id() {
        return program;
    }

    public boolean valid() {
        return valid;
    }

    public int uniform(String name) {
        return uniforms.computeIfAbsent(name, n -> GL20.glGetUniformLocation(program, n));
    }
}
