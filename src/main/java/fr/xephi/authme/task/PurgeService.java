package fr.xephi.authme.task;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.PurgeSettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import static fr.xephi.authme.util.StringUtils.makePath;

public class PurgeService implements Reloadable {

    @Inject
    private BukkitService bukkitService;

    @Inject
    private DataSource dataSource;

    @Inject
    private NewSetting settings;

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private PluginHooks pluginHooks;

    @Inject
    private Server server;

    private boolean isPurging = false;

    // Settings
    private int daysBeforePurge;

    /**
     * Return whether a purge is in progress.
     *
     * @return True if purging.
     */
    public boolean isPurging() {
        return this.isPurging;
    }

    /**
     * Set if a purge is currently in progress.
     *
     * @param purging True if purging.
     */
    void setPurging(boolean purging) {
        this.isPurging = purging;
    }

    /**
     * Purges players from the database. Run on startup if enabled.
     */
    public void runAutoPurge() {
        if (!settings.getProperty(PurgeSettings.USE_AUTO_PURGE)) {
            return;
        } else if (daysBeforePurge <= 0) {
            ConsoleLogger.showError("Did not run auto purge: configured days before purging must be positive");
            return;
        }

        ConsoleLogger.info("Automatically purging the database...");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -daysBeforePurge);
        long until = calendar.getTimeInMillis();

        runPurge(null, until);
    }

    /**
     * Run a purge with a specified time.
     *
     * @param sender Sender running the command.
     * @param until The minimum last login.
     */
    public void runPurge(CommandSender sender, long until) {
        //todo: note this should may run async because it may executes a SQL-Query
        Set<String> initialPurge = dataSource.getRecordsToPurge(until);
        if (CollectionUtils.isEmpty(initialPurge)) {
            logAndSendMessage(sender, "No players to purge");
            return;
        }

        Set<String> toPurge = getFinalPurgeList(initialPurge);
        purgePlayers(sender, toPurge, bukkitService.getOfflinePlayers());
    }

    /**
     * Purges the given list of player names.
     *
     * @param sender Sender running the command.
     * @param names The names to remove.
     * @param players Collection of OfflinePlayers (including those with the given names).
     */
    public void purgePlayers(CommandSender sender, Set<String> names, OfflinePlayer[] players) {
        //todo: note this should may run async because it may executes a SQL-Query
        if (isPurging) {
            logAndSendMessage(sender, "Purge is already in progress! Aborting purge request");
            return;
        }

        dataSource.purgeRecords(names);
        logAndSendMessage(sender, ChatColor.GOLD + "Deleted " + names.size() + " user accounts");
        logAndSendMessage(sender, ChatColor.GOLD + "Purging user accounts...");

        isPurging = true;
        PurgeTask purgeTask = new PurgeTask(this, sender, names, players);
        bukkitService.runTaskAsynchronously(purgeTask);
    }

    /**
     * Check each name in the initial purge findings to remove any player from the purge list
     * that has the bypass permission.
     *
     * @param initial The initial list of players to purge.
     *
     * @return The list of players to purge after permission check.
     */
    private Set<String> getFinalPurgeList(Set<String> initial) {
        Set<String> toPurge = new HashSet<>();

        for (String name : initial) {
            if (!permissionsManager.hasPermissionOffline(name, PlayerStatePermission.BYPASS_PURGE)) {
                toPurge.add(name);
            }
        }

        return toPurge;
    }

    synchronized void purgeAntiXray(Set<String> cleared) {
        if (!settings.getProperty(PurgeSettings.REMOVE_ANTI_XRAY_FILE)) {
            return;
        }

        int i = 0;
        File dataFolder = new File("." + File.separator + "plugins" + File.separator + "AntiXRayData"
            + File.separator + "PlayerData");
        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            return;
        }

        for (String file : dataFolder.list()) {
            if (cleared.contains(file.toLowerCase())) {
                File playerFile = new File(dataFolder, file);
                if (playerFile.exists() && playerFile.delete()) {
                    i++;
                }
            }
        }

        ConsoleLogger.info("AutoPurge: Removed " + i + " AntiXRayData Files");
    }

    synchronized void purgeLimitedCreative(Set<String> cleared) {
        if (!settings.getProperty(PurgeSettings.REMOVE_LIMITED_CREATIVE_INVENTORIES)) {
            return;
        }

        int i = 0;
        File dataFolder = new File("." + File.separator + "plugins" + File.separator + "LimitedCreative"
            + File.separator + "inventories");
        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            return;
        }
        for (String file : dataFolder.list()) {
            String name = file;
            int idx;
            idx = file.lastIndexOf("_creative.yml");
            if (idx != -1) {
                name = name.substring(0, idx);
            } else {
                idx = file.lastIndexOf("_adventure.yml");
                if (idx != -1) {
                    name = name.substring(0, idx);
                } else {
                    idx = file.lastIndexOf(".yml");
                    if (idx != -1) {
                        name = name.substring(0, idx);
                    }
                }
            }
            if (name.equals(file)) {
                continue;
            }
            if (cleared.contains(name.toLowerCase())) {
                File dataFile = new File(dataFolder, file);
                if (dataFile.exists() && dataFile.delete()) {
                    i++;
                }
            }
        }
        ConsoleLogger.info("AutoPurge: Removed " + i + " LimitedCreative Survival, Creative and Adventure files");
    }

    synchronized void purgeDat(Set<OfflinePlayer> cleared) {
        if (!settings.getProperty(PurgeSettings.REMOVE_PLAYER_DAT)) {
            return;
        }

        int i = 0;
        File dataFolder = new File(server.getWorldContainer()
            , makePath(settings.getProperty(PurgeSettings.DEFAULT_WORLD), "players"));

        for (OfflinePlayer offlinePlayer : cleared) {
            File playerFile = new File(dataFolder, Utils.getUUIDorName(offlinePlayer) + ".dat");
            if (playerFile.delete()) {
                i++;
            }
        }

        ConsoleLogger.info("AutoPurge: Removed " + i + " .dat Files");
    }

    /**
     * Method purgeEssentials.
     *
     * @param cleared List of String
     */
    synchronized void purgeEssentials(Set<OfflinePlayer> cleared) {
        if (!settings.getProperty(PurgeSettings.REMOVE_ESSENTIALS_FILES)) {
            return;
        }

        int i = 0;
        File essentialsDataFolder = pluginHooks.getEssentialsDataFolder();
        if (essentialsDataFolder == null) {
            ConsoleLogger.info("Cannot purge Essentials: plugin is not loaded");
            return;
        }

        final File userDataFolder = new File(essentialsDataFolder, "userdata");
        if (!userDataFolder.exists() || !userDataFolder.isDirectory()) {
            return;
        }

        for (OfflinePlayer offlinePlayer : cleared) {
            File playerFile = new File(userDataFolder, Utils.getUUIDorName(offlinePlayer) + ".yml");
            if (playerFile.exists() && playerFile.delete()) {
                i++;
            }
        }

        ConsoleLogger.info("AutoPurge: Removed " + i + " EssentialsFiles");
    }

    // TODO: What is this method for? Is it correct?
    // TODO: Make it work with OfflinePlayers group data.
    synchronized void purgePermissions(Set<OfflinePlayer> cleared) {
        if (!settings.getProperty(PurgeSettings.REMOVE_PERMISSIONS)) {
            return;
        }

        for (OfflinePlayer offlinePlayer : cleared) {
            String name = offlinePlayer.getName();
            permissionsManager.removeAllGroups(bukkitService.getPlayerExact(name));
        }

        ConsoleLogger.info("AutoPurge: Removed permissions from " + cleared.size() + " player(s).");
    }

    private static void logAndSendMessage(CommandSender sender, String message) {
        ConsoleLogger.info(message);
        // Make sure sender is not console user, which will see the message from ConsoleLogger already
        if (sender != null && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(message);
        }
    }

    @PostConstruct
    @Override
    public void reload() {
        this.daysBeforePurge = settings.getProperty(PurgeSettings.DAYS_BEFORE_REMOVE_PLAYER);
    }
}
