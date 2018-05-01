package fr.xephi.authme;

import com.google.common.base.Throwables;
import fr.xephi.authme.output.LogLevel;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.ExceptionUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The plugin's static logger.
 */
public final class ConsoleLogger {

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("[MM-dd HH:mm:ss]");
    private static Logger logger;
    private static LogLevel logLevel = LogLevel.INFO;
    private static boolean useLogging = false;
    private static File logFile;
    private static FileWriter fileWriter;

    private ConsoleLogger() {
    }

    // --------
    // Configurations
    // --------

    /**
     * Set the logger to use.
     *
     * @param logger The logger
     */
    public static void setLogger(Logger logger) {
        ConsoleLogger.logger = logger;
    }

    /**
     * Set the file to log to if enabled.
     *
     * @param logFile The log file
     */
    public static void setLogFile(File logFile) {
        ConsoleLogger.logFile = logFile;
    }

    /**
     * Load the required settings.
     *
     * @param settings The settings instance
     */
    public static void setLoggingOptions(Settings settings) {
        ConsoleLogger.logLevel = settings.getProperty(PluginSettings.LOG_LEVEL);
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


    // --------
    // Logging methods
    // --------

    /**
     * Log a WARN message.
     *
     * @param message The message to log
     */
    public static void warning(String message) {
        logger.warning(message);
        writeLog("[WARN] " + message);
    }

    /**
     * Log a Throwable with the provided message on WARNING level
     * and save the stack trace to the log file.
     *
     * @param message The message to accompany the exception
     * @param th      The Throwable to log
     */
    public static void logException(String message, Throwable th) {
        warning(message + " " + ExceptionUtils.formatException(th));
        writeLog(Throwables.getStackTraceAsString(th));
    }

    /**
     * Log an INFO message.
     *
     * @param message The message to log
     */
    public static void info(String message) {
        logger.info(message);
        writeLog("[INFO] " + message);
    }

    /**
     * Log a FINE message if enabled.
     * <p>
     * Implementation note: this logs a message on INFO level because
     * levels below INFO are disabled by Bukkit/Spigot.
     *
     * @param message The message to log
     */
    public static void fine(String message) {
        if (logLevel.includes(LogLevel.FINE)) {
            logger.info(message);
            writeLog("[FINE] " + message);
        }
    }

    // --------
    // Debug log methods
    // --------

    /**
     * Log a DEBUG message if enabled.
     * <p>
     * Implementation note: this logs a message on INFO level and prefixes it with "DEBUG" because
     * levels below INFO are disabled by Bukkit/Spigot.
     *
     * @param message The message to log
     */
    public static void debug(String message) {
        if (logLevel.includes(LogLevel.DEBUG)) {
            String debugMessage = "[DEBUG] " + message;
            logger.info(debugMessage);
            writeLog(debugMessage);
        }
    }

    /**
     * Log the DEBUG message from the supplier if enabled.
     *
     * @param msgSupplier the message supplier
     */
    public static void debug(Supplier<String> msgSupplier) {
        if (logLevel.includes(LogLevel.DEBUG)) {
            String debugMessage = "[DEBUG] " + msgSupplier.get();
            logger.info(debugMessage);
            writeLog(debugMessage);
        }
    }

    /**
     * Log the DEBUG message.
     *
     * @param message the message
     * @param param1 parameter to replace in the message
     */
    public static void debug(String message, Object param1) {
        if (logLevel.includes(LogLevel.DEBUG)) {
            String debugMessage = "[DEBUG] " + message;
            logger.log(Level.INFO, debugMessage, param1);
            writeLog(debugMessage + " {" + param1 + "}");
        }
    }

    /**
     * Log the DEBUG message.
     *
     * @param message the message
     * @param param1 first param to replace in message
     * @param param2 second param to replace in message
     */
    // Avoids array creation if DEBUG level is disabled
    public static void debug(String message, Object param1, Object param2) {
        if (logLevel.includes(LogLevel.DEBUG)) {
            debug(message, new Object[]{param1, param2});
        }
    }

    /**
     * Log the DEBUG message.
     *
     * @param message the message
     * @param params the params to replace in the message
     */
    public static void debug(String message, Object... params) {
        if (logLevel.includes(LogLevel.DEBUG)) {
            String debugMessage = "[DEBUG] " + message;
            logger.log(Level.INFO, debugMessage, params);
            writeLog(debugMessage + " {"
                + Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(", ")) + "}");
        }
    }


    // --------
    // Helpers
    // --------

    /**
     * Close all file handles.
     */
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

    /**
     * Write a message into the log file with a TimeStamp if enabled.
     *
     * @param message The message to write to the log
     */
    private static void writeLog(String message) {
        if (useLogging) {
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
    }
}
