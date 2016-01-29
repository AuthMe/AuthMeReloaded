package fr.xephi.authme;

import fr.xephi.authme.settings.Settings;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The backup management class
 *
 * @author stefano
 * @version $Revision: 1.0 $
 */
public class PerformBackup {

    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
    final String dateString = format.format(new Date());
    private final String dbName = Settings.getMySQLDatabase;
    private final String dbUserName = Settings.getMySQLUsername;
    private final String dbPassword = Settings.getMySQLPassword;
    private final String tblname = Settings.getMySQLTablename;
    private final String path = AuthMe.getInstance().getDataFolder() + File.separator + "backups" + File.separator + "backup" + dateString;
    private AuthMe instance;

    /**
     * Constructor for PerformBackup.
     *
     * @param instance AuthMe
     */
    public PerformBackup(AuthMe instance) {
        this.setInstance(instance);
    }

    /**
     * Perform a backup with the given reason.
     *
     * @param cause BackupCause The cause of the backup.
     */
    public void doBackup(BackupCause cause) {

        // Do nothing if backup is disabled
        if (!Settings.isBackupActivated) {
            // Print a warning if the backup was requested via command or by another plugin
            if (cause == BackupCause.COMMAND || cause == BackupCause.OTHER) {
                ConsoleLogger.showError("Can't perform a Backup: disabled in configuration. Cause of the Backup: " + cause.name());
            }
            return;
        }

        // Check whether a backup should be made at the specified point in time
        switch (cause) {
            case START:
                if (!Settings.isBackupOnStart)
                    return;
            case STOP:
                if (!Settings.isBackupOnStop)
                    return;
            case COMMAND:
            case OTHER:
        }

        // Do backup and check return value!
        if (doBackup()) {
            ConsoleLogger.info("A backup has been performed successfully. Cause of the Backup: " + cause.name());
        } else {
            ConsoleLogger.showError("Error while performing a backup! Cause of the Backup: " + cause.name());
        }
    }

    /**
     * Method doBackup.
     *
     * @return boolean
     */
    public boolean doBackup() {

        switch (Settings.getDataSource) {
            case FILE:
                return FileBackup("auths.db");
            case MYSQL:
                return MySqlBackup();
            case SQLITE:
                return FileBackup(Settings.getMySQLDatabase + ".db");
        }

        return false;
    }

    /**
     * Method MySqlBackup.
     *
     * @return boolean
     */
    private boolean MySqlBackup() {
        File dirBackup = new File(AuthMe.getInstance().getDataFolder() + "/backups");

        if (!dirBackup.exists())
            dirBackup.mkdir();
        if (checkWindows(Settings.backupWindowsPath)) {
            String executeCmd = Settings.backupWindowsPath + "\\bin\\mysqldump.exe -u " + dbUserName + " -p" + dbPassword + " " + dbName + " --tables " + tblname + " -r " + path + ".sql";
            Process runtimeProcess;
            try {
                runtimeProcess = Runtime.getRuntime().exec(executeCmd);
                int processComplete = runtimeProcess.waitFor();
                if (processComplete == 0) {
                    ConsoleLogger.info("Backup created successfully.");
                    return true;
                } else {
                    ConsoleLogger.showError("Could not create the backup!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
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
                    ConsoleLogger.showError("Could not create the backup!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Method FileBackup.
     *
     * @param backend String
     *
     * @return boolean
     */
    private boolean FileBackup(String backend) {
        File dirBackup = new File(AuthMe.getInstance().getDataFolder() + "/backups");

        if (!dirBackup.exists())
            dirBackup.mkdir();

        try {
            copy(new File("plugins" + File.separator + "AuthMe" + File.separator + backend), new File(path + ".db"));
            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Method checkWindows.
     *
     * @param windowsPath String
     *
     * @return boolean
     */
    private boolean checkWindows(String windowsPath) {
        String isWin = System.getProperty("os.name").toLowerCase();
        if (isWin.contains("win")) {
            if (new File(windowsPath + "\\bin\\mysqldump.exe").exists()) {
                return true;
            } else {
                ConsoleLogger.showError("Mysql Windows Path is incorrect please check it");
                return true;
            }
        } else return false;
    }

    /*
     * Check if we are under Windows and correct location of mysqldump.exe
     * otherwise return error.
     */

    /**
     * Method copy.
     *
     * @param src File
     * @param dst File
     *
     * @throws IOException
     */
    void copy(File src, File dst) throws IOException {
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

    /*
     * Copyr src bytefile into dst file
     */

    /**
     * Method getInstance.
     *
     * @return AuthMe
     */
    public AuthMe getInstance() {
        return instance;
    }

    /**
     * Method setInstance.
     *
     * @param instance AuthMe
     */
    public void setInstance(AuthMe instance) {
        this.instance = instance;
    }

    /**
     * Possible backup causes.
     */
    public enum BackupCause {
        START,
        STOP,
        COMMAND,
        OTHER,
    }

}
