package fr.xephi.authme.hooks;

import ch.jalu.injector.annotations.NoFieldScan;
import com.earth2me.essentials.Essentials;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import fr.xephi.authme.ConsoleLogger;
import net.minelink.ctplus.CombatTagPlus;
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
public class PluginHooks {

    private final PluginManager pluginManager;
    private Essentials essentials;
    private MultiverseCore multiverse;
    private CombatTagPlus combatTagPlus;

    /**
     * Constructor.
     *
     * @param pluginManager The server's plugin manager
     */
    @Inject
    public PluginHooks(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        tryHookToCombatPlus();
        tryHookToEssentials();
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

    /**
     * Checks whether the player is an NPC.
     *
     * @param player The player to process
     * @return True if player is NPC, false otherwise
     */
    public boolean isNpc(Player player) {
        return player.hasMetadata("NPC") || isNpcInCombatTagPlus(player);
    }

    /**
     * Query the CombatTagPlus plugin whether the given player is an NPC.
     *
     * @param player The player to verify
     * @return True if the player is an NPC according to CombatTagPlus, false if not or if the plugin is unavailable
     */
    private boolean isNpcInCombatTagPlus(Player player) {
        return combatTagPlus != null && combatTagPlus.getNpcPlayerHelper().isNpc(player);
    }


    // ------
    // "Is plugin available" methods
    // ------
    public boolean isEssentialsAvailable() {
        return essentials != null;
    }

    public boolean isMultiverseAvailable() {
        return multiverse != null;
    }

    public boolean isCombatTagPlusAvailable() {
        return combatTagPlus != null;
    }

    // ------
    // Hook methods
    // ------
    public void tryHookToEssentials() {
        try {
            essentials = getPlugin(pluginManager, "Essentials", Essentials.class);
        } catch (Exception | NoClassDefFoundError ignored) {
            essentials = null;
        }
    }

    public void tryHookToCombatPlus() {
        try {
            combatTagPlus = getPlugin(pluginManager, "CombatTagPlus", CombatTagPlus.class);
        } catch (Exception | NoClassDefFoundError ignored) {
            combatTagPlus = null;
        }
    }

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
    public void unhookEssentials() {
        essentials = null;
    }
    public void unhookCombatPlus() {
        combatTagPlus = null;
    }
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
