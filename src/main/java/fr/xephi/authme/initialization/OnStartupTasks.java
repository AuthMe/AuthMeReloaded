package fr.xephi.authme.initialization;

import ch.jalu.injector.exceptions.InjectorReflectionException;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;
import org.bstats.Metrics;
import fr.xephi.authme.output.ConsoleFilter;
import fr.xephi.authme.output.Log4JFilter;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Optional;
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

        metrics.addCustomChart(new Metrics.SimplePie("messages_language") {
            @Override
            public String getValue() {
                return settings.getProperty(PluginSettings.MESSAGES_LANGUAGE);
            }
        });

        metrics.addCustomChart(new Metrics.SimplePie("database_backend") {
            @Override
            public String getValue() {
                return settings.getProperty(DatabaseSettings.BACKEND).toString();
            }
        });
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
        bukkitService.runTaskTimerAsynchronously(new Runnable() {
            @Override
            public void run() {
                for (PlayerAuth auth : dataSource.getLoggedPlayers()) {
                    String email = auth.getEmail();
                    if (StringUtils.isEmpty(email) || "your@email.com".equalsIgnoreCase(email)) {
                        Player player = bukkitService.getPlayerExact(auth.getRealName());
                        if (player != null) {
                            messages.send(player, MessageKey.ADD_EMAIL_MESSAGE);
                        }
                    }
                }
            }
        }, 1, TICKS_PER_MINUTE * settings.getProperty(EmailSettings.DELAY_RECALL));
    }

    /**
     * Displays a hint to use the legacy AuthMe JAR if AuthMe could not be started
     * because Gson was not found.
     *
     * @param e the exception to process
     */
    public static void displayLegacyJarHint(Exception e) {
        if (e instanceof InjectorReflectionException) {
            Throwable causeOfCause = Optional.of(e)
                .map(Throwable::getCause)
                .map(Throwable::getCause).orElse(null);
            if (causeOfCause instanceof NoClassDefFoundError
                && "Lcom/google/gson/Gson;".equals(causeOfCause.getMessage())) {
                ConsoleLogger.warning("YOU MUST DOWNLOAD THE LEGACY JAR TO USE AUTHME ON YOUR SERVER");
                ConsoleLogger.warning("Get authme-legacy.jar from http://ci.xephi.fr/job/AuthMeReloaded/");
            }
        }
    }

    /**
     * Returns whether the hash algorithm is deprecated and won't be able
     * to be actively used anymore in 5.4.
     *
     * @param hash the hash algorithm to check
     * @return true if the hash will be deprecated, false otherwise
     * @see <a href="https://github.com/Xephi/AuthMeReloaded/issues/1016">#1016</a>
     */
    public static boolean isHashDeprecatedIn54(HashAlgorithm hash) {
        if (hash.getClazz() == null || hash == HashAlgorithm.PLAINTEXT) {
            // Exclude PLAINTEXT from this check because it already has a mandatory migration, which takes care of
            // sending all the necessary messages and warnings.
            return false;
        }

        Recommendation recommendation = hash.getClazz().getAnnotation(Recommendation.class);
        return recommendation != null && recommendation.value() == Usage.DEPRECATED;
    }
}
