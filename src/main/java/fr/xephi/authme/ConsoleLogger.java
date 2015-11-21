package fr.xephi.authme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import com.google.common.base.Throwables;

import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.settings.Settings;

/**
 */
public class ConsoleLogger {

    private static final Logger log = AuthMe.getInstance().getLogger();
    private static final DateFormat df = new SimpleDateFormat("[MM-dd HH:mm:ss]");

    /**
     * Method info.
     * @param message String
     */
    public static void info(String message) {
        log.info("[AuthMe] " + message);
        if (Settings.useLogging) {
            String dateTime;
            synchronized (df) {
                dateTime = df.format(new Date());
            }
            writeLog(dateTime + " " + message);
        }
    }

    /**
     * Method showError.
     * @param message String
     */
    public static void showError(String message) {
        log.warning("[AuthMe] " + message);
        if (Settings.useLogging) {
            String dateTime;
            synchronized (df) {
                dateTime = df.format(new Date());
            }
            writeLog(dateTime + " ERROR: " + message);
        }
    }

    /**
     * Method writeLog.
     * @param message String
     */
    public static void writeLog(String message) {
        try {
            Files.write(Settings.LOG_FILE.toPath(), (message + NewAPI.newline).getBytes(),
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE);
        } catch (IOException ignored) {
        }
    }

    /**
     * Method writeStackTrace.
     * @param ex Exception
     */
    public static void writeStackTrace(Exception ex) {
        if (Settings.useLogging) {
            String dateTime;
            synchronized (df) {
                dateTime = df.format(new Date());
            }
            writeLog(dateTime + " " + Throwables.getStackTraceAsString(ex));
        }
    }
}
