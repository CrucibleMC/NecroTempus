package io.github.cruciblemc.necrotempus.modules.features.bossbar.component;

import io.github.cruciblemc.necrotempus.api.bossbar.BossBarColor;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarType;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Data
public class BossDisplayAdapter {

    private final String targetClass;

    private final BossBarColor color;

    private final BossBarType type;

    private int lazyColor = -1;

    public BossDisplayAdapter(String targetClass, BossBarColor color, BossBarType type) {
        this.targetClass = targetClass;
        this.color = color;
        this.type = type;

        if (color == BossBarColor.LAZY) {
            lazyColor = color.intValue();
        }

    }

    public static List<BossDisplayAdapter> defaultList() {
        return Arrays.asList(
                new BossDisplayAdapter(
                        "net.minecraft.entity.boss.EntityDragon",
                        BossBarColor.PINK,
                        BossBarType.NOTCHED_10
                ),
                new BossDisplayAdapter(
                        "net.minecraft.entity.boss.EntityWither",
                        BossBarColor.PURPLE,
                        BossBarType.FLAT
                ),
                new BossDisplayAdapter(
                        "vazkii.botania.common.entity.EntityDoppleganger",
                        BossBarColor.PINK,
                        BossBarType.NONE
                ),
                new BossDisplayAdapter(
                        "noppes.npcs.entity.EntityCustomNpc",
                        BossBarColor.PINK,
                        BossBarType.NONE
                )
        );
    }

}
