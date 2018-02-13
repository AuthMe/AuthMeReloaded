package fr.xephi.authme.service;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.BackupSettings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.util.FileUtils;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static fr.xephi.authme.util.Utils.logAndSendMessage;
import static fr.xephi.authme.util.Utils.logAndSendWarning;

/**
 * Performs a backup of the data source.
 */
public class BackupService {

    private final File dataFolder;
    private final File backupFolder;
    private final Settings settings;

    /**
     * Constructor.
     *
     * @param dataFolder the data folder
     * @param settings the plugin settings
     */
    @Inject
    public BackupService(@DataFolder File dataFolder, Settings settings) {
        this.dataFolder = dataFolder;
        this.backupFolder = new File(dataFolder, "backups");
        this.settings = settings;
    }

    /**
     * Performs a backup for the given reason.
     *
     * @param cause backup reason
     */
    public void doBackup(BackupCause cause) {
        doBackup(cause, null);
    }

    /**
     * Performs a backup for the given reason.
     *
     * @param cause backup reason
     * @param sender the command sender (nullable)
     */
    public void doBackup(BackupCause cause, CommandSender sender) {
        if (!settings.getProperty(BackupSettings.ENABLED)) {
            // Print a warning if the backup was requested via command or by another plugin
            if (cause == BackupCause.COMMAND || cause == BackupCause.OTHER) {
                logAndSendWarning(sender,
                    "Can't perform a backup: disabled in configuration. Cause of the backup: " + cause.name());
            }
            return;
        } else if (BackupCause.START == cause && !settings.getProperty(BackupSettings.ON_SERVER_START)
                || BackupCause.STOP == cause && !settings.getProperty(BackupSettings.ON_SERVER_STOP)) {
            // Don't perform backup on start or stop if so configured
            return;
        }

        // Do backup and check return value!
        if (doBackup()) {
            logAndSendMessage(sender,
                "A backup has been performed successfully. Cause of the backup: " + cause.name());
        } else {
            logAndSendWarning(sender, "Error while performing a backup! Cause of the backup: " + cause.name());
        }
    }

    private boolean doBackup() {
        DataSourceType dataSourceType = settings.getProperty(DatabaseSettings.BACKEND);
        switch (dataSourceType) {
            case FILE:
                return performFileBackup("auths.db");
            case MYSQL:
                return performMySqlBackup();
            case SQLITE:
                String dbName = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
                return performFileBackup(dbName + ".db");
            default:
                ConsoleLogger.warning("Unknown data source type '" + dataSourceType + "' for backup");
        }

        return false;
    }

    private boolean performMySqlBackup() {
        FileUtils.createDirectory(backupFolder);
        File sqlBackupFile = constructBackupFile("sql");

        String backupWindowsPath = settings.getProperty(BackupSettings.MYSQL_WINDOWS_PATH);
        boolean isUsingWindows = useWindowsCommand(backupWindowsPath);
        String backupCommand = isUsingWindows
            ? backupWindowsPath + "\\bin\\mysqldump.exe" + buildMysqlDumpArguments(sqlBackupFile)
            : "mysqldump" + buildMysqlDumpArguments(sqlBackupFile);

        try {
            Process runtimeProcess = Runtime.getRuntime().exec(backupCommand);
            int processComplete = runtimeProcess.waitFor();
            if (processComplete == 0) {
                ConsoleLogger.info("Backup created successfully. (Using Windows = " + isUsingWindows + ")");
                return true;
            } else {
                ConsoleLogger.warning("Could not create the backup! (Using Windows = " + isUsingWindows + ")");
            }
        } catch (IOException | InterruptedException e) {
            ConsoleLogger.logException("Error during backup (using Windows = " + isUsingWindows + "):", e);
        }
        return false;
    }

    private boolean performFileBackup(String filename) {
        FileUtils.createDirectory(backupFolder);
        File backupFile = constructBackupFile("db");

        try {
            copy(new File(dataFolder, filename), backupFile);
            return true;
        } catch (IOException ex) {
            ConsoleLogger.logException("Encountered an error during file backup:", ex);
        }
        return false;
    }

    /**
     * Check if we are under Windows and correct location of mysqldump.exe
     * otherwise return error.
     *
     * @param windowsPath The path to check
     * @return True if the path is correct, false if it is incorrect or the OS is not Windows
     */
    private static boolean useWindowsCommand(String windowsPath) {
        String isWin = System.getProperty("os.name").toLowerCase();
        if (isWin.contains("win")) {
            if (new File(windowsPath + "\\bin\\mysqldump.exe").exists()) {
                return true;
            } else {
                ConsoleLogger.warning("Mysql Windows Path is incorrect. Please check it");
                return false;
            }
        }
        return false;
    }

    /**
     * Builds the command line arguments to pass along when running the {@code mysqldump} command.
     *
     * @param sqlBackupFile the file to back up to
     * @return the mysqldump command line arguments
     */
    private String buildMysqlDumpArguments(File sqlBackupFile) {
        String dbUsername = settings.getProperty(DatabaseSettings.MYSQL_USERNAME);
        String dbPassword = settings.getProperty(DatabaseSettings.MYSQL_PASSWORD);
        String dbName     = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
        String tableName  = settings.getProperty(DatabaseSettings.MYSQL_TABLE);

        return " -u " + dbUsername + " -p" + dbPassword + " " + dbName
            + " --tables " + tableName + " -r " + sqlBackupFile.getPath() + ".sql";
    }

    /**
     * Constructs the file name to back up the data source to.
     *
     * @param fileExtension the file extension to use (e.g. sql)
     * @return the file to back up the data to
     */
    private File constructBackupFile(String fileExtension) {
        String dateString = FileUtils.createCurrentTimeString();
        return new File(backupFolder, "backup" + dateString + "." + fileExtension);
    }

    private static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    /**
     * Possible backup causes.
     */
    public enum BackupCause {
        START,
        STOP,
        COMMAND,
        OTHER
    }

}
