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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;

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

    private boolean autoPurging = false;

    // Settings
    private boolean useAutoPurge;
    private boolean removeEssentialsFiles;
    private boolean removePlayerDat;
    private boolean removeLimitedCreativeInventories;
    private boolean removeAntiXrayFiles;
    private boolean removePermissions;
    private int daysBeforePurge;

    /**
     * Return whether an automatic purge is in progress.
     *
     * @return True if purging.
     */
    public boolean isAutoPurging() {
        return this.autoPurging;
    }

    /**
     * Set if an automatic purge is currently in progress.
     *
     * @param autoPurging True if automatically purging.
     */
    void setAutoPurging(boolean autoPurging) {
        this.autoPurging = autoPurging;
    }

    /**
     * Purges players from the database. Ran on startup.
     */
    public void runAutoPurge() {
        if (!useAutoPurge || autoPurging) {
            return;
        }

        this.autoPurging = true;

        // Get the initial list of players to purge
        ConsoleLogger.info("Automatically purging the database...");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, daysBeforePurge);
        long until = calendar.getTimeInMillis();
        Set<String> initialPurge = dataSource.getRecordsToPurge(until);

        if (CollectionUtils.isEmpty(initialPurge)) {
            return;
        }

        // Remove players from the purge list if they have bypass permission
        Set<String> toPurge = getFinalPurgeList(initialPurge);

        // Purge players from the database
        dataSource.purgeRecords(toPurge);
        ConsoleLogger.info("Purged the database: " + toPurge.size() + " accounts removed!");
        ConsoleLogger.info("Purging user accounts...");

        // Schedule a PurgeTask
        PurgeTask purgeTask = new PurgeTask(this, Bukkit.getConsoleSender(), toPurge, Bukkit.getOfflinePlayers(), true);
        bukkitService.runTaskAsynchronously(purgeTask);
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
            return;
        }

        Set<String> toPurge = getFinalPurgeList(initialPurge);

        // Purge records from the database
        dataSource.purgeRecords(toPurge);
        sender.sendMessage(ChatColor.GOLD + "Deleted " + toPurge.size() + " user accounts");
        sender.sendMessage(ChatColor.GOLD + "Purging user accounts...");

        // Schedule a PurgeTask
        PurgeTask purgeTask = new PurgeTask(this, sender, toPurge, Bukkit.getOfflinePlayers(), false);
        bukkitService.runTaskAsynchronously(purgeTask);
    }

    public void purgeBanned(CommandSender sender, Set<String> bannedNames, Set<OfflinePlayer> bannedPlayers) {
        //todo: note this should may run async because it may executes a SQL-Query
        dataSource.purgeBanned(bannedNames);

        OfflinePlayer[] bannedPlayersArray = new OfflinePlayer[bannedPlayers.size()];
        bannedPlayers.toArray(bannedPlayersArray);
        PurgeTask purgeTask = new PurgeTask(this, sender, bannedNames, bannedPlayersArray, false);
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
            if (!permissionsManager.hasPermission(name, PlayerStatePermission.BYPASS_PURGE)) {
                toPurge.add(name);
            }
        }

        return toPurge;
    }

    synchronized void purgeAntiXray(Set<String> cleared) {
        if (!removeAntiXrayFiles) {
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
        if (!removeLimitedCreativeInventories) {
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
        if (!removePlayerDat) {
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
        if (!removeEssentialsFiles && !pluginHooks.isEssentialsAvailable()) {
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
        if (!removePermissions) {
            return;
        }

        for (OfflinePlayer offlinePlayer : cleared) {
            String name = offlinePlayer.getName();
            permissionsManager.removeAllGroups(bukkitService.getPlayerExact(name));
        }

        ConsoleLogger.info("AutoPurge: Removed permissions from " + cleared.size() + " player(s).");
    }

    @PostConstruct
    @Override
    public void reload() {
        this.useAutoPurge = settings.getProperty(PurgeSettings.USE_AUTO_PURGE);
        this.removeEssentialsFiles = settings.getProperty(PurgeSettings.REMOVE_ESSENTIALS_FILES);
        this.removePlayerDat = settings.getProperty(PurgeSettings.REMOVE_PLAYER_DAT);
        this.removeAntiXrayFiles = settings.getProperty(PurgeSettings.REMOVE_ANTI_XRAY_FILE);
        this.removeLimitedCreativeInventories = settings.getProperty(PurgeSettings.REMOVE_LIMITED_CREATIVE_INVENTORIES);
        this.removePermissions = settings.getProperty(PurgeSettings.REMOVE_PERMISSIONS);
        this.daysBeforePurge = settings.getProperty(PurgeSettings.DAYS_BEFORE_REMOVE_PLAYER);
    }
}
