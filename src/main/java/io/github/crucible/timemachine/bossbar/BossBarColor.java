package io.github.crucible.timemachine.bossbar;

public enum BossBarColor {

    PINK("pink"),
    BLUE("blue"),
    RED("red"),
    GREEN("green"),
    YELLOW("yellow"),
    PURPLE("purple"),
    WHITE("white");

    private final String color;

    public String getColor() {
        return color;
    }

    BossBarColor(String color){
        this.color = color;
    }

}
