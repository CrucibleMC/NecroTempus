package io.github.crucible.necrotempus.modules.bossbar.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;

import java.util.Arrays;
import java.util.List;

import static io.github.crucible.necrotempus.modules.bossbar.api.BossBarColor.*;
import static io.github.crucible.necrotempus.modules.bossbar.api.BossBarType.*;

@Data
@AllArgsConstructor
public class BossDisplayAdapter {

    private String targetClass;

    private BossBarColor color;

    private BossBarType type;

    public static List<BossDisplayAdapter> defaultList(){
        return Arrays.asList(
                new BossDisplayAdapter(EntityDragon.class.getName(), PINK, NOTCHED_10),
                new BossDisplayAdapter(EntityWither.class.getName(), PURPLE, FLAT)
        );
    }

}
