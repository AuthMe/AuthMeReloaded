package fr.xephi.authme;

import com.google.common.base.Throwables;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.settings.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class ConsoleLogger {

    private static final Logger log = AuthMe.getInstance().getLogger();
    private static final DateFormat df = new SimpleDateFormat("[MM-dd HH:mm:ss]");

    public static void info(String message) {
        if (AuthMe.getInstance().isEnabled()) {
            log.info("[AuthMe] " + message);
            if (Settings.useLogging) {
                String dateTime;
                synchronized (df) {
                    dateTime = df.format(new Date());
                }
                writeLog(dateTime + " " + message);
            }
        }
    }

    public static void showError(String message) {
        if (AuthMe.getInstance().isEnabled()) {
            log.warning("[AuthMe] " + message);
            if (Settings.useLogging) {
                String dateTime;
                synchronized (df) {
                    dateTime = df.format(new Date());
                }
                writeLog(dateTime + " ERROR: " + message);
            }
        }
    }

    public static void writeLog(String message) {
        try {
            Files.write(Settings.LOG_FILE.toPath(), (message + NewAPI.newline).getBytes(),
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE);
        } catch (IOException ignored) {
        }
    }

    public static void writeStackTrace(Exception ex) {
        String dateTime;
        synchronized (df) {
            dateTime = df.format(new Date());
        }
        writeLog(dateTime + " " + Throwables.getStackTraceAsString(ex));
    }
}
