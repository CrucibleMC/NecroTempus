package io.github.cruciblemc.necrotempus;

import com.avaje.ebean.EbeanServer;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.*;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class NecroTempusPlugin implements Plugin {

    private java.util.logging.Logger pluginLogger;
    private PluginLoader dummyPluginLoader;
    private PluginDescriptionFile dummyPluginDescription;
    private boolean isPluginEnabled = false;

    @Override
    public File getDataFolder() {
        return new File("/plugins", "forge");
    }


    @Override
    public PluginDescriptionFile getDescription() {
        if (dummyPluginDescription == null) {
            dummyPluginDescription = new PluginDescriptionFile(Tags.MODNAME, Tags.VERSION, NecroTempusPlugin.class.getName());
        }
        return dummyPluginDescription;
    }

    @Override
    public FileConfiguration getConfig() {
        return null;
    }

    @Override
    public InputStream getResource(String s) {
        return null;
    }

    @Override
    public void saveConfig() {

    }

    @Override
    public void saveDefaultConfig() {

    }

    @Override
    public void saveResource(String s, boolean b) {

    }

    @Override
    public void reloadConfig() {

    }

    @Override
    public PluginLoader getPluginLoader() {
        if (dummyPluginLoader == null) {
            dummyPluginLoader = new PluginLoader() {
                @Override
                public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
                    try {
                        return getServer().getPluginManager().loadPlugin(file);
                    } catch (InvalidDescriptionException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                public PluginDescriptionFile getPluginDescription(File file) {
                    if (dummyPluginDescription == null) {
                        dummyPluginDescription = new PluginDescriptionFile(Tags.MODNAME, Tags.VERSION, NecroTempusPlugin.class.getName());
                    }
                    return dummyPluginDescription;
                }

                @Override
                public Pattern[] getPluginFileFilters() {
                    return new Pattern[0];
                }

                @Override
                public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, Plugin plugin) {
                    return null;
                }

                @Override
                public void enablePlugin(Plugin plugin) {
                    isPluginEnabled = true;
                }

                @Override
                public void disablePlugin(Plugin plugin) {
                    if (!MinecraftServer.getServer().isServerRunning()) isPluginEnabled = false;
                }
            };
        }
        return dummyPluginLoader;
    }

    @Override
    public Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public boolean isEnabled() {
        return isPluginEnabled;
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onLoad() {
        getLogger().info("The NecroTempus plugin loaded successfully!");
    }

    @Override
    public void onEnable() {

    }

    @Override
    public boolean isNaggable() {
        return false;
    }

    @Override
    public void setNaggable(boolean b) {

    }

    @Override
    public EbeanServer getDatabase() {
        return null;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String s, String s1) {
        return null;
    }

    @Override
    public java.util.logging.Logger getLogger() {
        if (pluginLogger == null)
            pluginLogger = new PluginLogger(this);
        return pluginLogger;
    }

    @Override
    public String getName() {
        return Tags.MODNAME;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return Collections.emptyList();
    }
}
