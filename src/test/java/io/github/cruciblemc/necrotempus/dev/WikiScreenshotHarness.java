package io.github.cruciblemc.necrotempus.dev;

import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.mojang.authlib.GameProfile;

import io.github.cruciblemc.necrotempus.api.actionbar.ActionBar;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBar;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarColor;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBarType;
import io.github.cruciblemc.necrotempus.api.glow.GlowManager;
import io.github.cruciblemc.necrotempus.api.playertab.PlayerTab;
import io.github.cruciblemc.necrotempus.api.playertab.TabCell;
import io.github.cruciblemc.necrotempus.api.title.TitleComponent;
import io.github.cruciblemc.necrotempus.api.title.TitleElement;

public class WikiScreenshotHarness extends JavaPlugin {

    private BossBar activeBossBar;
    private Pig glowTarget;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        WikiScreenshotScenario.Scenario scenario = WikiScreenshotScenario.parse(arguments);
        if (scenario == null) {
            sender.sendMessage("Usage: /ntwiki <bossbar|title|actionbar|playertab|glow|clear>");
            return true;
        }
        if (!sender.hasPermission("necrotempus.wiki.capture")) {
            sender.sendMessage("You do not have permission to run screenshot cases.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Run this case as a player.");
            return true;
        }

        Player player = (Player) sender;
        switch (scenario) {
            case BOSSBAR:
                showBossBar(player);
                break;
            case TITLE:
                showTitle(player);
                break;
            case ACTIONBAR:
                showActionBar(player);
                break;
            case PLAYERTAB:
                showPlayerTab(player);
                break;
            case GLOW:
                showGlow(player);
                break;
            case CLEAR:
                clear(player);
                break;
            default:
                throw new IllegalStateException("Unhandled screenshot scenario: " + scenario);
        }
        return true;
    }

    private void showBossBar(Player player) {
        java.util.UUID playerId = player.getUniqueId();
        if (activeBossBar != null) BossBar.getBossBarManager()
            .removePlayer(playerId, activeBossBar);

        activeBossBar = BossBar.createBossBar();
        activeBossBar.setText(new ChatComponentText("Wiki capture: boss bar"));
        activeBossBar.setColor(BossBarColor.BLUE);
        activeBossBar.setType(BossBarType.NOTCHED_10);
        activeBossBar.setPercentage(0.72F);
        activeBossBar.setVisible(true);
        BossBar.getBossBarManager()
            .addPlayer(playerId, activeBossBar);
    }

    private void showTitle(Player player) {
        TitleComponent title = new TitleComponent();
        title.setFadeIn(250);
        title.setStay(8000);
        title.setFadeOut(500);
        title.addElement(TitleElement.titleOf(new ChatComponentText("Wiki capture title")));
        title.addElement(TitleElement.subtitleOf(new ChatComponentText("Title and subtitle")));
        TitleComponent.getTitleManager()
            .set(viewers(player), title);
    }

    private void showActionBar(Player player) {
        ActionBar.getActionBarManager()
            .set(viewers(player), new ActionBar(160, new ChatComponentText("Wiki capture: action bar")));
    }

    private void showPlayerTab(Player player) {
        TabCell cell = new TabCell(
            new ChatComponentText(player.getName()),
            player.getName(),
            new GameProfile(player.getUniqueId(), player.getName()),
            false,
            42);
        PlayerTab tab = new PlayerTab(
            Collections.singletonList(cell),
            true,
            new ChatComponentText("Wiki capture: player list"),
            new ChatComponentText("Header, head, ping, footer"));
        PlayerTab.getPlayerTabManager()
            .set(viewers(player), tab);
    }

    private void showGlow(Player player) {
        removeGlowTarget();
        Location location = player.getLocation();
        Vector direction = location.getDirection()
            .setY(0)
            .normalize()
            .multiply(2.5);
        location.add(direction);
        glowTarget = player.getWorld()
            .spawn(location, Pig.class);
        glowTarget.setCustomName("Wiki Glow Target");
        glowTarget.setCustomNameVisible(true);
        // Zero keeps the manual validation active until /ntwiki clear.
        GlowManager.glow(handle(player), glowTarget.getEntityId(), 0x44AAFF, 0);
    }

    private void clear(Player player) {
        HashSet<UUID> viewers = viewers(player);
        ActionBar.getActionBarManager()
            .remove(viewers);
        TitleComponent.getTitleManager()
            .remove(viewers);
        PlayerTab.getPlayerTabManager()
            .remove(viewers);
        GlowManager.clear(handle(player));
        removeGlowTarget();
        if (activeBossBar != null) {
            BossBar.getBossBarManager()
                .removePlayer(player.getUniqueId(), activeBossBar);
            activeBossBar = null;
        }
    }

    private void removeGlowTarget() {
        if (glowTarget != null) {
            glowTarget.remove();
            glowTarget = null;
        }
    }

    private static EntityPlayerMP handle(Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    private static HashSet<UUID> viewers(Player player) {
        return new HashSet<>(Collections.singleton(player.getUniqueId()));
    }
}
