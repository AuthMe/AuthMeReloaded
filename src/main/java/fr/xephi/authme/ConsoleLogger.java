package fr.xephi.authme;

import com.google.common.base.Throwables;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Wrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The plugin's static logger.
 */
public final class ConsoleLogger {

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("[MM-dd HH:mm:ss]");

    private static Wrapper wrapper = Wrapper.getInstance();

    private ConsoleLogger() {
        // Service class
    }

    /**
     * Print an info message.
     *
     * @param message String
     */
    public static void info(String message) {
        wrapper.getLogger().info(message);
        if (Settings.useLogging) {
            writeLog(message);
        }
    }

    /**
     * Print an error message.
     *
     * @param message String
     */
    public static void showError(String message) {
        wrapper.getLogger().warning(message);
        if (Settings.useLogging) {
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
            Files.write(Settings.LOG_FILE.toPath(), (dateTime + ": " + message + NEW_LINE).getBytes(),
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
        if (Settings.useLogging) {
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
