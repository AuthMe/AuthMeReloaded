package fr.xephi.authme.initialization;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.output.ConsoleFilter;
import fr.xephi.authme.output.Log4JFilter;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.apache.logging.log4j.LogManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.logging.Logger;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_MINUTE;
import static fr.xephi.authme.settings.properties.EmailSettings.RECALL_PLAYERS;

/**
 * Contains actions such as migrations that should be performed on startup.
 */
public class OnStartupTasks {

    @Inject
    private DataSource dataSource;
    @Inject
    private Settings settings;
    @Inject
    private BukkitService bukkitService;
    @Inject
    private Messages messages;

    OnStartupTasks() {
    }

    /**
     * Sends bstats metrics.
     *
     * @param plugin the plugin instance
     * @param settings the settings
     */
    public static void sendMetrics(AuthMe plugin, Settings settings) {
        final Metrics metrics = new Metrics(plugin);

        metrics.addCustomChart(new Metrics.SimplePie("messages_language",
            () -> settings.getProperty(PluginSettings.MESSAGES_LANGUAGE)));
        metrics.addCustomChart(new Metrics.SimplePie("database_backend",
            () -> settings.getProperty(DatabaseSettings.BACKEND).toString()));
    }

    /**
     * Sets up the console filter if enabled.
     *
     * @param settings the settings
     * @param logger   the plugin logger
     */
    public static void setupConsoleFilter(Settings settings, Logger logger) {
        if (!settings.getProperty(SecuritySettings.REMOVE_PASSWORD_FROM_CONSOLE)) {
            return;
        }
        // Try to set the log4j filter
        try {
            Class.forName("org.apache.logging.log4j.core.filter.AbstractFilter");
            setLog4JFilter();
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // log4j is not available
            ConsoleLogger.info("You're using Minecraft 1.6.x or older, Log4J support will be disabled");
            ConsoleFilter filter = new ConsoleFilter();
            logger.setFilter(filter);
            Bukkit.getLogger().setFilter(filter);
            Logger.getLogger("Minecraft").setFilter(filter);
        }
    }

    /**
     * Check if argon2 library is available into java.library.path
     *
     * @return true if the library exist in library path, false otherwise
     */
    public static boolean checkArgon2Presence() {
        try {
            System.loadLibrary("argon2");
            return true;
        } catch (UnsatisfiedLinkError e) {
            ConsoleLogger.logException("Cannot find libargon2", e);
        }
        return false;
    }

    // Set the console filter to remove the passwords
    private static void setLog4JFilter() {
        org.apache.logging.log4j.core.Logger logger;
        logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        logger.addFilter(new Log4JFilter());
    }

    public void scheduleRecallEmailTask() {
        if (!settings.getProperty(RECALL_PLAYERS)) {
            return;
        }
        bukkitService.runTaskTimerAsynchronously(() -> {
            for (String playerWithoutMail : dataSource.getLoggedPlayersWithEmptyMail()) {
                Player player = bukkitService.getPlayerExact(playerWithoutMail);
                if (player != null) {
                    messages.send(player, MessageKey.ADD_EMAIL_MESSAGE);
                }
            }
        }, 1, TICKS_PER_MINUTE * settings.getProperty(EmailSettings.DELAY_RECALL));
    }
}
