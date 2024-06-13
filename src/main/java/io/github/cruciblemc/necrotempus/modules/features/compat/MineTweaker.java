package io.github.cruciblemc.necrotempus.modules.features.compat;

import io.github.cruciblemc.necrotempus.NecroTempus;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.compat.crafttweaker.BossBar;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.compat.crafttweaker.Glyphs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class MineTweaker {

    public static void init() {
        try {

            Class<?> MT_API = Class.forName("minetweaker.MineTweakerAPI");
            Method method = MT_API.getDeclaredMethod("registerClass", Class.class);

            for (Class<?> clazz : Arrays.asList(BossBar.class, Glyphs.class))
                method.invoke(null, clazz);

        } catch (NoClassDefFoundError | ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            NecroTempus.getInstance().getLogger().warn("CraftTweaker is not available.");
        }
    }

}
