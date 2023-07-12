package io.github.cruciblemc.necrotempus.modules.mixin.mixins.bukkit.boss;

import io.github.cruciblemc.necrotempus.api.bossbar.BossBar;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarColor;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarType;
import net.minecraft.util.ChatComponentText;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mixin(value = org.bukkit.craftbukkit.v1_7_R4.boss.CraftBossBar.class, remap = false)
public class CraftBossBar {

    public BossBar INNERBossBar;

    @Inject(method = "<init>(Ljava/lang/String;Lorg/bukkit/boss/BarColor;Lorg/bukkit/boss/BarStyle;[Lorg/bukkit/boss/BarFlag;)V", at = @At("RETURN"))
    public void init(String par1, BarColor par2, BarStyle par3, BarFlag[] par4, CallbackInfo ci) {

        INNERBossBar = BossBar.createBossBar(
                UUID.randomUUID(),
                new ChatComponentText(par1),
                BossBarColor.valueOfString(par2.name()),
                BossBarType.valueOfString(par3.name()),
                0F,
                true
        );
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public String getTitle() {
        return INNERBossBar.getText().getUnformattedTextForChat();
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public void setTitle(String title) {
        INNERBossBar.setText(new ChatComponentText(title));
        BossBar.getBossBarManager().sync(INNERBossBar);
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public BarColor getColor() {
        return BarColor.valueOf(INNERBossBar.getColor().name());
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public void setColor(BarColor color) {
        INNERBossBar.setColor(BossBarColor.valueOfString(color.name()));
        BossBar.getBossBarManager().sync(INNERBossBar);
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public BarStyle getStyle() {
        return BarStyle.valueOf(INNERBossBar.getType().name());
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public void setStyle(BarStyle style) {
        INNERBossBar.setType(BossBarType.valueOfString(style.name()));
        BossBar.getBossBarManager().sync(INNERBossBar);
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public void addFlag(BarFlag flag) {
        //
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public void removeFlag(BarFlag flag) {
        //
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public boolean hasFlag(BarFlag flag) {
        return true;
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public void setProgress(double progress) {
        INNERBossBar.setPercentage((float) progress);
        BossBar.getBossBarManager().sync(INNERBossBar);
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public double getProgress() {
        return INNERBossBar.getPercentage();
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public void addPlayer(Player player) {
        BossBar.getBossBarManager().addPlayer(player.getUniqueId(), INNERBossBar);
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public void removePlayer(Player player) {
        BossBar.getBossBarManager().removePlayer(player.getUniqueId(), INNERBossBar);
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public List<Player> getPlayers() {
        return BossBar.getBossBarManager().getPlayers(INNERBossBar).stream().map(Bukkit::getPlayer).collect(Collectors.toList());
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public void setVisible(boolean visible) {
        INNERBossBar.setVisible(visible);
        BossBar.getBossBarManager().sync(INNERBossBar);
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public boolean isVisible() {
        return INNERBossBar.isVisible();
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public void show() {
        setVisible(true);
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public void hide() {
        setVisible(false);
    }

    /**
     * @author Brunoxkk0
     * @reason BossBar Wrapper Implementation
     */
    @Overwrite
    public void removeAll() {
        BossBar.getBossBarManager().removeAllPlayers(INNERBossBar);
    }

}
