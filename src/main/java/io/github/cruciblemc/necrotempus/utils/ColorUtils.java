package io.github.cruciblemc.necrotempus.utils;

import java.awt.*;

public class ColorUtils {

    public static Color decodeColor(String hex) {

        hex = hex.toLowerCase();

        if (hex.startsWith("#") || hex.startsWith("x"))
            hex = hex.substring(1);

        int r = Integer.valueOf(hex.substring(0, 2), 16);
        int g = Integer.valueOf(hex.substring(2, 4), 16);
        int b = Integer.valueOf(hex.substring(4, 6), 16);
        int a = -1;

        if (hex.length() >= 8) {
            a = Integer.valueOf(hex.substring(6, 8), 16);
        }

        if (a != -1)
            return new Color(r, g, b, a);
        else
            return new Color(r, g, b);
    }

    public static boolean isShadow(int currentColor) {
        return currentColor == 0 ||
                currentColor == 42 ||
                currentColor == 10752 ||
                currentColor == 10794 ||
                currentColor == 2752512 ||
                currentColor == 2752554 ||
                currentColor == 2763264 ||
                currentColor == 2763306 ||
                currentColor == 1381653 ||
                currentColor == 1381695 ||
                currentColor == 1392405 ||
                currentColor == 1392447 ||
                currentColor == 4134165 ||
                currentColor == 4134207 ||
                currentColor == 4144917 ||
                currentColor == 4144959;
    }

}
