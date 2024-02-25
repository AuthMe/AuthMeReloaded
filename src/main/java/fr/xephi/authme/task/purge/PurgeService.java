package fr.xephi.authme.task.purge;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PurgeSettings;
import fr.xephi.authme.util.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static fr.xephi.authme.util.Utils.logAndSendMessage;

/**
 * Initiates purge tasks.
 */
public class PurgeService {
    
    private final ConsoleLogger logger = ConsoleLoggerFactory.get(PurgeService.class);

    @Inject
    private BukkitService bukkitService;

    @Inject
    private DataSource dataSource;

    @Inject
    private Settings settings;

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private PurgeExecutor purgeExecutor;

    /** Keeps track of whether a purge task is currently running. */
    private boolean isPurging = false;

    PurgeService() {
    }

    /**
     * Purges players from the database. Runs on startup if enabled.
     */
    public void runAutoPurge() {
        int daysBeforePurge = settings.getProperty(PurgeSettings.DAYS_BEFORE_REMOVE_PLAYER);
        if (!settings.getProperty(PurgeSettings.USE_AUTO_PURGE)) {
            return;
        } else if (daysBeforePurge <= 0) {
            logger.warning("Did not run auto purge: configured days before purging must be positive");
            return;
        }

        logger.info("Automatically purging the database...");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -daysBeforePurge);
        long until = calendar.getTimeInMillis();

        runPurge(null, until);
    }

    /**
     * Runs a purge with a specified last login threshold. Players who haven't logged in since the threshold
     * will be purged.
     *
     * @param sender Sender running the command
     * @param until The last login threshold in milliseconds
     */
    public void runPurge(CommandSender sender, long until) {
        //todo: note this should may run async because it may executes a SQL-Query
        Set<String> toPurge = dataSource.getRecordsToPurge(until);
        if (Utils.isCollectionEmpty(toPurge)) {
            logAndSendMessage(sender, "No players to purge");
            return;
        }

        purgePlayers(sender, toPurge, bukkitService.getOfflinePlayers());
    }

    /**
     * Purges the given list of player names.
     *
     * @param sender Sender running the command
     * @param names The names to remove
     * @param players Collection of OfflinePlayers (including those with the given names)
     */
    public void purgePlayers(CommandSender sender, Set<String> names, OfflinePlayer[] players) {
        if (isPurging) {
            logAndSendMessage(sender, "Purge is already in progress! Aborting purge request");
            return;
        }

        isPurging = true;
        PurgeTask purgeTask = new PurgeTask(this, permissionsManager, sender, names, players);
        bukkitService.runOnAsyncSchedulerAtFixedRate(purgeTask, 0, 50L, TimeUnit.MILLISECONDS);
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
     * Perform purge operations for the given players and names.
     *
     * @param players the players (associated with the names)
     * @param names the lowercase names
     */
    void executePurge(Collection<OfflinePlayer> players, Collection<String> names) {
        purgeExecutor.executePurge(players, names);
    }
}
