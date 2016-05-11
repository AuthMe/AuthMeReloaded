package fr.xephi.authme;

import com.google.common.base.Throwables;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * The plugin's static logger.
 */
public final class ConsoleLogger {

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("[MM-dd HH:mm:ss]");
    private static Logger logger;
    private static boolean useLogging = false;
    private static File logFile;

    private ConsoleLogger() {
        // Service class
    }

    public static void setLogger(Logger logger) {
        ConsoleLogger.logger = logger;
    }

    public static void setLoggingOptions(boolean useLogging, File logFile) {
        ConsoleLogger.useLogging = useLogging;
        ConsoleLogger.logFile = logFile;
    }

    /**
     * Print an info message.
     *
     * @param message String
     */
    public static void info(String message) {
        logger.info(message);
        if (useLogging) {
            writeLog(message);
        }
    }

    public static void debug(String message) {
        if (!AuthMe.getInstance().getSettings().getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
            logger.fine(message);
            if (useLogging) {
                writeLog("Debug: " + message);
            }
        }
    }

    /**
     * Print an error message.
     *
     * @param message String
     */
    public static void showError(String message) {
        logger.warning(message);
        if (useLogging) {
            writeLog("ERROR: " + message);
        }
    }

    /**
     * Write a message into the log file with a TimeStamp.
     *
     * @param message String
     */
    private static void writeLog(String message) {
        String dateTime;
        synchronized (DATE_FORMAT) {
            dateTime = DATE_FORMAT.format(new Date());
        }
        try {
            Files.write(logFile.toPath(), (dateTime + ": " + message + NEW_LINE).getBytes(),
                StandardOpenOption.APPEND,
                StandardOpenOption.CREATE);
        } catch (IOException ignored) {
        }
    }

    /**
     * Write a StackTrace into the log.
     *
     * @param th The Throwable whose stack trace should be logged
     */
    public static void writeStackTrace(Throwable th) {
        if (useLogging) {
            writeLog(Throwables.getStackTraceAsString(th));
        }
    }

    /**
     * Logs a Throwable with the provided message and saves the stack trace to the log file.
     *
     * @param message The message to accompany the exception
     * @param th The Throwable to log
     */
    public static void logException(String message, Throwable th) {
        showError(message + " " + StringUtils.formatException(th));
        writeStackTrace(th);
    }
}
