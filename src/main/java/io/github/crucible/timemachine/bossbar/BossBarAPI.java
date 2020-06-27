package io.github.crucible.timemachine.bossbar;

import io.github.crucible.timemachine.bossbar.server.BossBar;
import io.github.crucible.timemachine.bossbar.server.BossBarTemp;
import net.minecraft.util.ChatComponentText;

import java.util.Random;
import java.util.UUID;

public class BossBarAPI {

    public static boolean READY = true;

    private static final Random random = new Random();

    private static final int colors = BossBarColor.values().length;
    private static final int types = BossBarType.values().length;

    public static BossBar createRandom(UUID uuid, ChatComponentText componentText, Float percentage){

        BossBar bar = new BossBar(componentText, BossBarColor.WHITE, BossBarType.NOTCHED_12, percentage,true, uuid);

        bar.setColor(BossBarColor.values()[random.nextInt(colors)]);
        bar.setType(BossBarType.values()[random.nextInt(types)]);

        return  bar;
    }

    public static BossBarTemp createRandomTemp(UUID uuid, ChatComponentText componentText, Float percentage){

        BossBarTemp bar = new BossBarTemp(componentText, BossBarColor.WHITE, BossBarType.NOTCHED_12, percentage,true, uuid);

        bar.setColor(BossBarColor.values()[random.nextInt(colors)]);
        bar.setType(BossBarType.values()[random.nextInt(types)]);

        return  bar;
    }

    public static BossBarTemp createTemp(UUID uuid, ChatComponentText componentText, Float percentage, BossBarColor bossBarColor, BossBarType bossBarType){

        BossBarTemp bar = new BossBarTemp(componentText, BossBarColor.WHITE, BossBarType.NOTCHED_12, percentage,true, uuid);

        bar.setColor(bossBarColor);
        bar.setType(bossBarType);

        return  bar;
    }

}
