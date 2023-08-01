package io.github.cruciblemc.necrotempus.modules.features.bossbar.component;

import io.github.cruciblemc.necrotempus.api.bossbar.BossBarColor;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Data
@AllArgsConstructor
public class BossDisplayAdapter {

    private String targetClass;

    private BossBarColor color;

    private BossBarType type;

    public static List<BossDisplayAdapter> defaultList(){
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
