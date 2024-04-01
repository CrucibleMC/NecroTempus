package io.github.cruciblemc.necrotempus.api.bossbar;


import io.github.cruciblemc.necrotempus.utils.ColorUtils;
import lombok.Getter;

public enum BossBarColor {

    PINK("pink", 15466679),
    BLUE("blue", 47084),
    RED("red", 15480064),
    GREEN("green", 1960960),
    YELLOW("yellow", 15264768),
    PURPLE("purple", 8061164),
    WHITE("white", 16777215),
    LAZY("lazy", 47084);

    @Getter
    private final String identifier;

    private int color;

    BossBarColor(String colorIdentifier, int color) {
        this.identifier = colorIdentifier;
        this.color = color;
    }

    public static BossBarColor lazyOf(int color) {
        BossBarColor barColor = LAZY;
        barColor.color = color;
        return barColor;
    }

    public static BossBarColor lazyOf(String color) {
        BossBarColor barColor = LAZY;
        barColor.color = ColorUtils.decodeColor(color).getRGB();
        return barColor;
    }

    public int intValue() {
        return color;
    }

    public static BossBarColor valueOfString(String name) {

        if (name.startsWith("$")) {
            return lazyOf(Integer.parseInt(name.substring(1)));
        }

        for (BossBarColor type : values()) {
            if (type.identifier.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return PINK;
    }

    @Override
    public String toString() {
        return "BossBarColor{" +
                "colorIdentifier='" + identifier + '\'' +
                ", color=" + color +
                '}';
    }
}
