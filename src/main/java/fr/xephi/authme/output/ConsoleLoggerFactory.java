package fr.xephi.authme.output;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Settings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates and keeps track of {@link ConsoleLogger} instances.
 */
public final class ConsoleLoggerFactory {

    private static final Map<String, ConsoleLogger> consoleLoggers = new ConcurrentHashMap<>();
    private static Settings settings;

    private ConsoleLoggerFactory() {
    }

    /**
     * Creates or returns the already existing logger associated with the given class.
     *
     * @param owningClass the class whose logger should be retrieved
     * @return logger for the given class
     */
    public static ConsoleLogger get(Class<?> owningClass) {
        String name = owningClass.getCanonicalName();
        return consoleLoggers.computeIfAbsent(name, ConsoleLoggerFactory::createLogger);
    }

    /**
     * Sets up all loggers according to the properties returned by the settings instance.
     *
     * @param settings the settings instance
     */
    public static void reloadSettings(Settings settings) {
        ConsoleLoggerFactory.settings = settings;
        ConsoleLogger.initializeSharedSettings(settings);

        consoleLoggers.values()
            .forEach(logger -> logger.initializeSettings(settings));
    }

    public static int getTotalLoggers() {
        return consoleLoggers.size();
    }

    private static ConsoleLogger createLogger(String name) {
        ConsoleLogger logger = new ConsoleLogger(name);
        if (settings != null) {
            logger.initializeSettings(settings);
        }
        return logger;
    }
}
