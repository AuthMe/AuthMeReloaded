package fr.xephi.authme;

import com.google.common.base.Throwables;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.StringUtils;

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
public class ConsoleLogger {

    private static final Logger log = AuthMe.getInstance().getLogger();
    private static final DateFormat df = new SimpleDateFormat("[MM-dd HH:mm:ss]");

    /**
     * Returns the plugin's logger.
     *
     * @return Logger
     */
    public static Logger getLogger() {
        return log;
    }

    /**
     * Print an info message.
     *
     * @param message String
     */
    public static void info(String message) {
        log.info(message);
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
        log.warning(message);
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
