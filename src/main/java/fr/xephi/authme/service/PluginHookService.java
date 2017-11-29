package fr.xephi.authme.service;

import ch.jalu.injector.annotations.NoFieldScan;
import com.earth2me.essentials.Essentials;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import fr.xephi.authme.ConsoleLogger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import javax.inject.Inject;
import java.io.File;

/**
 * Hooks into third-party plugins and allows to perform actions on them.
 */
@NoFieldScan
public class PluginHookService {

    private final PluginManager pluginManager;
    private Essentials essentials;
    private Plugin cmi;
    private MultiverseCore multiverse;

    /**
     * Constructor.
     *
     * @param pluginManager The server's plugin manager
     */
    @Inject
    public PluginHookService(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        tryHookToEssentials();
        tryHookToCmi();
        tryHookToMultiverse();
    }

    /**
     * Enable or disable the social spy status of the given user if Essentials is available.
     *
     * @param player The player to modify
     * @param socialSpyStatus The social spy status (enabled/disabled) to set
     */
    public void setEssentialsSocialSpyStatus(Player player, boolean socialSpyStatus) {
        if (essentials != null) {
            essentials.getUser(player).setSocialSpyEnabled(socialSpyStatus);
        }
    }

    /**
     * If Essentials is hooked into, return Essentials' data folder.
     *
     * @return The Essentials data folder, or null if unavailable
     */
    public File getEssentialsDataFolder() {
        if (essentials != null) {
            return essentials.getDataFolder();
        }
        return null;
    }

    /**
     * If CMI is hooked into, return CMI' data folder.
     *
     * @return The CMI data folder, or null if unavailable
     */
    public File getCmiDataFolder() {
        Plugin plugin = pluginManager.getPlugin("CMI");
        if(plugin == null) {
            return null;
        }
        return plugin.getDataFolder();
    }

    /**
     * Return the spawn of the given world as defined by Multiverse (if available).
     *
     * @param world The world to get the Multiverse spawn for
     * @return The spawn location from Multiverse, or null if unavailable
     */
    public Location getMultiverseSpawn(World world) {
        if (multiverse != null) {
            MVWorldManager manager = multiverse.getMVWorldManager();
            if (manager.isMVWorld(world)) {
                return manager.getMVWorld(world).getSpawnLocation();
            }
        }
        return null;
    }

    // ------
    // "Is plugin available" methods
    // ------

    /**
     * @return true if we have a hook to Essentials, false otherwise
     */
    public boolean isEssentialsAvailable() {
        return essentials != null;
    }

    /**
     * @return true if we have a hook to CMI, false otherwise
     */
    public boolean isCmiAvailable() {
        return cmi != null;
    }

    /**
     * @return true if we have a hook to Multiverse, false otherwise
     */
    public boolean isMultiverseAvailable() {
        return multiverse != null;
    }

    // ------
    // Hook methods
    // ------

    /**
     * Attempts to create a hook into Essentials.
     */
    public void tryHookToEssentials() {
        try {
            essentials = getPlugin(pluginManager, "Essentials", Essentials.class);
        } catch (Exception | NoClassDefFoundError ignored) {
            essentials = null;
        }
    }

    /**
     * Attempts to create a hook into CMI.
     */
    public void tryHookToCmi() {
        try {
            cmi = getPlugin(pluginManager, "CMI", Plugin.class);
        } catch (Exception | NoClassDefFoundError ignored) {
            cmi = null;
        }
    }

    /**
     * Attempts to create a hook into Multiverse.
     */
    public void tryHookToMultiverse() {
        try {
            multiverse = getPlugin(pluginManager, "Multiverse-Core", MultiverseCore.class);
        } catch (Exception | NoClassDefFoundError ignored) {
            multiverse = null;
        }
    }

    // ------
    // Unhook methods
    // ------

    /**
     * Unhooks from Essentials.
     */
    public void unhookEssentials() {
        essentials = null;
    }

    /**
     * Unhooks from CMI.
     */
    public void unhookCmi() {
        cmi = null;
    }

    /**
     * Unhooks from Multiverse.
     */
    public void unhookMultiverse() {
        multiverse = null;
    }

    // ------
    // Helpers
    // ------

    private static <T extends Plugin> T getPlugin(PluginManager pluginManager, String name, Class<T> clazz)
        throws Exception, NoClassDefFoundError {
        if (pluginManager.isPluginEnabled(name)) {
            T plugin = clazz.cast(pluginManager.getPlugin(name));
            ConsoleLogger.info("Hooked successfully into " + name);
            return plugin;
        }
        return null;
    }

}
