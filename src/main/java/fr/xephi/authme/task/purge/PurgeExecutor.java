package fr.xephi.authme.task.purge;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.player.NamedIdentifier;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PurgeSettings;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static fr.xephi.authme.util.FileUtils.makePath;

/**
 * Executes the purge operations.
 */
public class PurgeExecutor {

    @Inject
    private Settings settings;

    @Inject
    private DataSource dataSource;

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private PluginHookService pluginHookService;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private Server server;

    PurgeExecutor() {
    }

    /**
     * Performs the purge operations, i.e. deletes data and removes the files associated with the given
     * players and names.
     *
     * @param players the players to purge
     * @param names   names to purge
     */
    public void executePurge(Collection<OfflinePlayer> players, Collection<NamedIdentifier> names) {
        // Purge other data
        purgeFromAuthMe(names);
        purgeEssentials(players);
        purgeDat(players);
        purgePermissions(players);
    }

    /**
     * Deletes the given accounts from AuthMe.
     *
     * @param names the name of the accounts to delete
     */
    synchronized void purgeFromAuthMe(Collection<NamedIdentifier> names) {
        dataSource.purgeRecords(names);
        //TODO ljacqu 20160717: We shouldn't output namedBanned.size() but the actual total that was deleted
        ConsoleLogger.info(ChatColor.GOLD + "Deleted " + names.size() + " user accounts");
    }

    /**
     * Removes the .dat file of the given players.
     *
     * @param cleared list of players to clear
     */
    synchronized void purgeDat(Collection<OfflinePlayer> cleared) {
        if (!settings.getProperty(PurgeSettings.REMOVE_PLAYER_DAT)) {
            return;
        }

        int i = 0;
        File dataFolder = new File(server.getWorldContainer(),
            makePath(settings.getProperty(PurgeSettings.DEFAULT_WORLD), "players"));

        for (OfflinePlayer offlinePlayer : cleared) {
            File playerFile = new File(dataFolder, offlinePlayer.getUniqueId() + ".dat");
            if (playerFile.delete()) {
                i++;
            }
        }

        ConsoleLogger.info("AutoPurge: Removed " + i + " .dat Files");
    }

    /**
     * Removes the Essentials userdata file of each given player.
     *
     * @param cleared list of players to clear
     */
    synchronized void purgeEssentials(Collection<OfflinePlayer> cleared) {
        if (!settings.getProperty(PurgeSettings.REMOVE_ESSENTIALS_FILES)) {
            return;
        }

        File essentialsDataFolder = pluginHookService.getEssentialsDataFolder();
        if (essentialsDataFolder == null) {
            ConsoleLogger.info("Cannot purge Essentials: plugin is not loaded");
            return;
        }

        final File userDataFolder = new File(essentialsDataFolder, "userdata");
        if (!userDataFolder.exists() || !userDataFolder.isDirectory()) {
            return;
        }

        int deletedFiles = 0;
        for (OfflinePlayer offlinePlayer : cleared) {
            File playerFile = new File(userDataFolder, offlinePlayer.getUniqueId() + ".yml");
            if (playerFile.exists() && playerFile.delete()) {
                deletedFiles++;
            }
        }

        ConsoleLogger.info("AutoPurge: Removed " + deletedFiles + " EssentialsFiles");
    }

    /**
     * Removes permission data (groups a user belongs to) for the given players.
     *
     * @param cleared the players to remove data for
     */
    synchronized void purgePermissions(Collection<OfflinePlayer> cleared) {
        if (!settings.getProperty(PurgeSettings.REMOVE_PERMISSIONS)) {
            return;
        }

        for (OfflinePlayer offlinePlayer : cleared) {
            if (!permissionsManager.loadUserData(offlinePlayer)) {
                ConsoleLogger.warning("Unable to purge the permissions of user " + offlinePlayer + "!");
                continue;
            }
            permissionsManager.removeAllGroups(offlinePlayer);
        }

        ConsoleLogger.info("AutoPurge: Removed permissions from " + cleared.size() + " player(s).");
    }
}
