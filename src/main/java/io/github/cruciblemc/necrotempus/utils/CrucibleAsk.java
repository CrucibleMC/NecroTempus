package io.github.cruciblemc.necrotempus.utils;

public class CrucibleAsk {

    public static boolean isAvailable() {
        try {
            CrucibleAsk.class.getClassLoader().loadClass("io.github.crucible.api.CrucibleAPI");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

}
