package io.github.cruciblemc.necrotempus.modules.features.bossbar.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.cruciblemc.necrotempus.api.bossbar.BossBar;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.UUID;

@SideOnly(Side.CLIENT)
public class ClientBossBarManager {

    private static ClientBossBarManager instance;

    private ClientBossBarManager() {
        instance = this;
    }

    public static ClientBossBarManager getInstance() {
        return (instance != null) ? instance : new ClientBossBarManager();
    }

    private static final LinkedHashMap<UUID, BossBar> BOSS_BARS_ENTRIES = new LinkedHashMap<>();

    public static void add(BossBar bossBar) {
        BOSS_BARS_ENTRIES.put(bossBar.getUuid(), bossBar);
    }

    public static void remove(BossBar bossBar) {
        BOSS_BARS_ENTRIES.remove(bossBar.getUuid());
    }

    public static Iterator<BossBar> iterator() {
        return BOSS_BARS_ENTRIES.values().iterator();
    }

    public static boolean isEmpty() {
        return BOSS_BARS_ENTRIES.isEmpty();
    }

    public static int size() {
        return BOSS_BARS_ENTRIES.size();
    }

    public static void clear(){
        BOSS_BARS_ENTRIES.clear();
    }

    @SubscribeEvent
    public void onPlayerQuit(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        clear();
    }

}
