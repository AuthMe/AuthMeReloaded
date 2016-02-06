package fr.xephi.authme;

import org.mockito.Mockito;

import java.util.logging.Logger;

/**
 * Test initializer for {@link ConsoleLogger}.
 */
public class ConsoleLoggerTestInitializer {

    private ConsoleLoggerTestInitializer() {
    }

    public static Logger setupLogger() {
        Logger logger = Mockito.mock(Logger.class);
        ConsoleLogger.setLogger(logger);
        return logger;
    }
}
