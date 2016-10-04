package fr.xephi.authme.service;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSourceType;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.BackupSettings;
import fr.xephi.authme.settings.properties.DatabaseSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The backup management class
 *
 * @author stefano
 */
public class BackupService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm");

    private final String dbName;
    private final String dbUserName;
    private final String dbPassword;
    private final String tblname;
    private final String path;
    private final File dataFolder;
    private final Settings settings;

    /**
     * Constructor for PerformBackup.
     *
     * @param instance AuthMe
     * @param settings The plugin settings
     */
    public BackupService(AuthMe instance, Settings settings) {
        this.dataFolder = instance.getDataFolder();
        this.settings   = settings;
        this.dbName     = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
        this.dbUserName = settings.getProperty(DatabaseSettings.MYSQL_USERNAME);
        this.dbPassword = settings.getProperty(DatabaseSettings.MYSQL_PASSWORD);
        this.tblname    = settings.getProperty(DatabaseSettings.MYSQL_TABLE);

        String dateString = DATE_FORMAT.format(new Date());
        this.path = String.join(File.separator,
            instance.getDataFolder().getPath(), "backups", "backup" + dateString);
    }

    /**
     * Perform a backup with the given reason.
     *
     * @param cause The cause of the backup.
     */
    public void doBackup(BackupCause cause) {
        if (!settings.getProperty(BackupSettings.ENABLED)) {
            // Print a warning if the backup was requested via command or by another plugin
            if (cause == BackupCause.COMMAND || cause == BackupCause.OTHER) {
                ConsoleLogger.warning("Can't perform a Backup: disabled in configuration. Cause of the Backup: "
                    + cause.name());
            }
            return;
        }

        // Check whether a backup should be made at the specified point in time
        if (BackupCause.START.equals(cause) && !settings.getProperty(BackupSettings.ON_SERVER_START)
            || BackupCause.STOP.equals(cause) && !settings.getProperty(BackupSettings.ON_SERVER_STOP)) {
            return;
        }

        // Do backup and check return value!
        if (doBackup()) {
            ConsoleLogger.info("A backup has been performed successfully. Cause of the Backup: " + cause.name());
        } else {
            ConsoleLogger.warning("Error while performing a backup! Cause of the Backup: " + cause.name());
        }
    }

    public boolean doBackup() {
        DataSourceType dataSourceType = settings.getProperty(DatabaseSettings.BACKEND);
        switch (dataSourceType) {
            case FILE:
                return fileBackup("auths.db");
            case MYSQL:
                return mySqlBackup();
            case SQLITE:
                return fileBackup(dbName + ".db");
            default:
                ConsoleLogger.warning("Unknown data source type '" + dataSourceType + "' for backup");
        }

        return false;
    }

    private boolean mySqlBackup() {
        File dirBackup = new File(dataFolder + File.separator + "backups");

        if (!dirBackup.exists()) {
            dirBackup.mkdir();
        }
        String backupWindowsPath = settings.getProperty(BackupSettings.MYSQL_WINDOWS_PATH);
        if (checkWindows(backupWindowsPath)) {
            String executeCmd = backupWindowsPath + "\\bin\\mysqldump.exe -u " + dbUserName + " -p" + dbPassword + " " + dbName + " --tables " + tblname + " -r " + path + ".sql";
            Process runtimeProcess;
            try {
                runtimeProcess = Runtime.getRuntime().exec(executeCmd);
                int processComplete = runtimeProcess.waitFor();
                if (processComplete == 0) {
                    ConsoleLogger.info("Backup created successfully.");
                    return true;
                } else {
                    ConsoleLogger.warning("Could not create the backup! (Windows)");
                }
            } catch (IOException | InterruptedException e) {
                ConsoleLogger.logException("Error during Windows backup:", e);
            }
        } else {
            String executeCmd = "mysqldump -u " + dbUserName + " -p" + dbPassword + " " + dbName + " --tables " + tblname + " -r " + path + ".sql";
            Process runtimeProcess;
            try {
                runtimeProcess = Runtime.getRuntime().exec(executeCmd);
                int processComplete = runtimeProcess.waitFor();
                if (processComplete == 0) {
                    ConsoleLogger.info("Backup created successfully.");
                    return true;
                } else {
                    ConsoleLogger.warning("Could not create the backup!");
                }
            } catch (IOException | InterruptedException e) {
                ConsoleLogger.logException("Error during backup:", e);
            }
        }
        return false;
    }

    private boolean fileBackup(String backend) {
        File dirBackup = new File(dataFolder + File.separator + "backups");

        if (!dirBackup.exists())
            dirBackup.mkdir();

        try {
            copy("plugins" + File.separator + "AuthMe" + File.separator + backend, path + ".db");
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
    private static boolean checkWindows(String windowsPath) {
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

    private static void copy(String src, String dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
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
