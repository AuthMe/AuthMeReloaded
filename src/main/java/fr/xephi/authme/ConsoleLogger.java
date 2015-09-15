package fr.xephi.authme;

import com.google.common.base.Throwables;
import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.settings.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class ConsoleLogger {

    private static final Logger log = AuthMe.getInstance().getLogger();


    public static void info(String message) {
        if (AuthMe.getInstance().isEnabled()) {
            log.info("[AuthMe] " + message);
            if (Settings.useLogging) {
                writeLog("[" + DateFormat.getDateTimeInstance().format(new Date()) + "] " + message);
            }
        }
    }

    public static void showError(String message) {
        if (AuthMe.getInstance().isEnabled()) {
            log.warning("[AuthMe] " + message);
            if (Settings.useLogging) {
                writeLog("[" + DateFormat.getDateTimeInstance().format(new Date()) + "] ERROR: " + message);
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
        writeLog(Throwables.getStackTraceAsString(ex));
    }
}
