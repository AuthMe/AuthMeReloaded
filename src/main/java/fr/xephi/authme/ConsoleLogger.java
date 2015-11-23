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

    private static Wrapper wrapper = new Wrapper(AuthMe.getInstance());
    private static final DateFormat df = new SimpleDateFormat("[MM-dd HH:mm:ss]");

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
        if (!Settings.useLogging) {
            return;
        }
        writeLog("" + message);
    }

    /**
     * Print an error message.
     *
     * @param message String
     */
    public static void showError(String message) {
        wrapper.getLogger().warning(message);
        if (!Settings.useLogging) {
            return;
        }
        writeLog("ERROR: " + message);
    }

    /**
     * Write a message into the log file with a TimeStamp.
     *
     * @param message String
     */
    private static void writeLog(String message) {
        String dateTime;
        synchronized (df) {
            dateTime = df.format(new Date());
        }
        try {
            Files.write(Settings.LOG_FILE.toPath(), (dateTime + ": " + message + StringUtils.newline).getBytes(),
                StandardOpenOption.APPEND,
                StandardOpenOption.CREATE);
        } catch (IOException ignored) {
        }
    }

    /**
     * Write a StackTrace into the log.
     *
     * @param ex Exception
     */
    public static void writeStackTrace(Exception ex) {
        if (!Settings.useLogging) {
            return;
        }
        writeLog("" + Throwables.getStackTraceAsString(ex));
    }
}
