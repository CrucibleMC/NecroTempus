package io.github.cruciblemc.necrotempus.modules.features.bossbar.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.entity.boss.EntityWither;
import net.minecraftforge.client.event.RenderLivingEvent;

import org.junit.jupiter.api.Test;

import io.github.cruciblemc.necrotempus.api.bossbar.BossBar;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarColor;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarType;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.client.ClientBossBarManager;
import io.github.cruciblemc.necrotempus.modules.features.bossbar.component.BossDisplayAdapter;

class BossDisplayAdapterListenerTest {

    @Test
    void scriptOverridesWitherDefaultsAndUndoRestoresPreviousStyle() {
        EntityWither wither = new EntityWither(null);
        BossDisplayAdapter script = new BossDisplayAdapter(
            "net.minecraft.entity.boss.EntityWither",
            BossBarColor.lazyOf(8388736),
            BossBarType.NOTCHED_10);
        BossDisplayAdapter laterScript = new BossDisplayAdapter(
            "net.minecraft.entity.boss.EntityWither",
            BossBarColor.GREEN,
            BossBarType.NOTCHED_6);
        BossDisplayAdapter repeatedScript = new BossDisplayAdapter(
            "net.minecraft.entity.boss.EntityWither",
            BossBarColor.lazyOf(8388736),
            BossBarType.NOTCHED_10);

        try {
            BossDisplayAdapterListener.add(script);
            BossBar bar = render(wither);
            assertEquals(BossBarType.NOTCHED_10, bar.getType());
            assertEquals(8388736, bar.getLazyColor());

            BossDisplayAdapterListener.add(laterScript);
            bar = render(wither);
            assertEquals(BossBarType.NOTCHED_6, bar.getType());
            assertEquals(BossBarColor.GREEN, bar.getColor());
            assertEquals(-1, bar.getLazyColor());

            BossDisplayAdapterListener.add(repeatedScript);
            bar = render(wither);
            assertEquals(BossBarType.NOTCHED_10, bar.getType());
            assertEquals(8388736, bar.getLazyColor());

            BossDisplayAdapterListener.remove(repeatedScript);
            bar = render(wither);
            assertEquals(BossBarType.NOTCHED_6, bar.getType());
            assertEquals(-1, bar.getLazyColor());

            BossDisplayAdapterListener.remove(laterScript);
            bar = render(wither);
            assertEquals(BossBarType.NOTCHED_10, bar.getType());
            assertEquals(8388736, bar.getLazyColor());

            BossDisplayAdapterListener.remove(script);
            bar = render(wither);
            assertEquals(BossBarType.FLAT, bar.getType());
            assertEquals(BossBarColor.PURPLE, bar.getColor());
            assertEquals(-1, bar.getLazyColor());
        } finally {
            BossDisplayAdapterListener.remove(repeatedScript);
            BossDisplayAdapterListener.remove(laterScript);
            BossDisplayAdapterListener.remove(script);
            ClientBossBarManager.clear();
        }
    }

    private static BossBar render(EntityWither wither) {
        ClientBossBarManager.clear();
        BossDisplayAdapterListener.getInstance()
            .onRenderLiving(new RenderLivingEvent.Pre(wither, null, 0, 0, 0));
        return ClientBossBarManager.iterator()
            .next();
    }
}
