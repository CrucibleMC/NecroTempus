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

}
