package fr.xephi.authme;

import com.google.common.base.Throwables;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private static FileWriter fileWriter;

    private ConsoleLogger() {
    }

    public static void setLogger(Logger logger) {
        ConsoleLogger.logger = logger;
    }

    public static void setLogFile(File logFile) {
        ConsoleLogger.logFile = logFile;
    }

    public static void setLoggingOptions(NewSetting settings) {
        ConsoleLogger.useLogging = settings.getProperty(SecuritySettings.USE_LOGGING);
        if (useLogging) {
            if (fileWriter == null) {
                try {
                    fileWriter = new FileWriter(logFile, true);
                } catch (IOException e) {
                    ConsoleLogger.logException("Failed to create the log file:", e);
                }
            }
        } else {
            close();
        }
    }

    /**
     * Print an info message.
     *
     * @param message String
     */
    public static void info(String message) {
        logger.info(message);
        if (useLogging) {
            writeLog("[INFO] " + message);
        }

    }

    /**
     * Print an error message.
     *
     * @param message String
     */
    public static void warning(String message) {
        logger.warning(message);
        if (useLogging) {
            writeLog("[WARN] " + message);
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
            fileWriter.write(dateTime);
            fileWriter.write(": ");
            fileWriter.write(message);
            fileWriter.write(NEW_LINE);
            fileWriter.flush();
        } catch (IOException ignored) {
        }
    }

    /**
     * Logs a Throwable with the provided message and saves the stack trace to the log file.
     *
     * @param message The message to accompany the exception
     * @param th      The Throwable to log
     */
    public static void logException(String message, Throwable th) {
        warning(message + " " + StringUtils.formatException(th));
        if (useLogging) {
            writeLog(Throwables.getStackTraceAsString(th));
        }
    }

    public static void close() {
        if (fileWriter != null) {
            try {
                fileWriter.flush();
                fileWriter.close();
                fileWriter = null;
            } catch (IOException ignored) {
            }
        }
    }
}
