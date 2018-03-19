package fr.xephi.authme.datasource;

import com.google.common.io.Files;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migrates the SQLite database when necessary.
 */
class SqLiteMigrater {

    private final File dataFolder;
    private final String databaseName;
    private final String tableName;
    private final Columns col;

    SqLiteMigrater(Settings settings, File dataFolder) {
        this.dataFolder = dataFolder;
        this.databaseName = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        this.col = new Columns(settings);
    }

    /**
     * Returns whether the database needs to be migrated.
     * <p>
     * Background: Before commit 22911a0 (July 2016), new SQLite databases initialized the last IP column to be NOT NULL
     * without a default value. Allowing the last IP to be null (#792) is therefore not compatible.
     *
     * @param metaData the database meta data
     * @param tableName the table name (SQLite file name)
     * @param col column names configuration
     * @return true if a migration is necessary, false otherwise
     */
    static boolean isMigrationRequired(DatabaseMetaData metaData, String tableName, Columns col) throws SQLException {
        return SqlDataSourceUtils.isNotNullColumn(metaData, tableName, col.LAST_IP)
            && SqlDataSourceUtils.getColumnDefaultValue(metaData, tableName, col.LAST_IP) == null;
    }

    /**
     * Migrates the given SQLite instance.
     *
     * @param sqLite the instance to migrate
     */
    void performMigration(SQLite sqLite) throws SQLException {
        ConsoleLogger.warning("YOUR SQLITE DATABASE NEEDS MIGRATING! DO NOT TURN OFF YOUR SERVER");

        String backupName = createBackup();
        ConsoleLogger.info("Made a backup of your database at 'backups/" + backupName + "'");

        recreateDatabaseWithNewDefinitions(sqLite);
        ConsoleLogger.info("SQLite database migrated successfully");
    }

    private String createBackup() {
        File sqLite = new File(dataFolder, databaseName + ".db");
        File backupDirectory = new File(dataFolder, "backups");
        FileUtils.createDirectory(backupDirectory);

        String backupName = "backup-" + databaseName + FileUtils.createCurrentTimeString() + ".db";
        File backup = new File(backupDirectory, backupName);
        try {
            Files.copy(sqLite, backup);
            return backupName;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create SQLite backup before migration", e);
        }
    }

    /**
     * Renames the current database, creates a new database under the name and copies the data
     * from the renamed database to the newly created one. This is necessary because SQLite
     * does not support dropping or modifying a column.
     *
     * @param sqLite the SQLite instance to migrate
     */
    // cf. https://stackoverflow.com/questions/805363/how-do-i-rename-a-column-in-a-sqlite-database-table
    private void recreateDatabaseWithNewDefinitions(SQLite sqLite) throws SQLException {
        Connection connection = getConnection(sqLite);
        String tempTable = "tmp_" + tableName;
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE " + tableName + " RENAME TO " + tempTable + ";");
        }

        sqLite.reload();
        connection = getConnection(sqLite);

        try (Statement st = connection.createStatement()) {
            String copySql = "INSERT INTO $table ($id, $name, $realName, $password, $lastIp, $lastLogin, $regIp, "
                + "$regDate, $locX, $locY, $locZ, $locWorld, $locPitch, $locYaw, $email, $isLogged)"
                + "SELECT $id, $name, $realName,"
                + " $password, CASE WHEN $lastIp = '127.0.0.1' OR $lastIp = '' THEN NULL else $lastIp END,"
                + " $lastLogin, $regIp, $regDate, $locX, $locY, $locZ, $locWorld, $locPitch, $locYaw,"
                + " CASE WHEN $email = 'your@email.com' THEN NULL ELSE $email END, $isLogged"
                + " FROM " + tempTable + ";";
            int insertedEntries = st.executeUpdate(replaceColumnVariables(copySql));
            ConsoleLogger.info("Copied over " + insertedEntries + " from the old table to the new one");

            st.execute("DROP TABLE " + tempTable + ";");
        }
    }

    private String replaceColumnVariables(String sql) {
        String replacedSql = sql.replace("$table", tableName).replace("$id", col.ID)
            .replace("$name", col.NAME).replace("$realName", col.REAL_NAME)
            .replace("$password", col.PASSWORD).replace("$lastIp", col.LAST_IP)
            .replace("$lastLogin", col.LAST_LOGIN).replace("$regIp", col.REGISTRATION_IP)
            .replace("$regDate", col.REGISTRATION_DATE).replace("$locX", col.LASTLOC_X)
            .replace("$locY", col.LASTLOC_Y).replace("$locZ", col.LASTLOC_Z)
            .replace("$locWorld", col.LASTLOC_WORLD).replace("$locPitch", col.LASTLOC_PITCH)
            .replace("$locYaw", col.LASTLOC_YAW).replace("$email", col.EMAIL)
            .replace("$isLogged", col.IS_LOGGED);
        if (replacedSql.contains("$")) {
            throw new IllegalStateException("SQL still statement still has '$' in it - was a tag not replaced?"
                + " Replacement result: " + replacedSql);
        }
        return replacedSql;
    }

    /**
     * Returns the connection from the given SQLite instance.
     *
     * @param sqLite the SQLite instance to process
     * @return the connection to the SQLite database
     */
    private static Connection getConnection(SQLite sqLite) {
        try {
            Field connectionField = SQLite.class.getDeclaredField("con");
            connectionField.setAccessible(true);
            return (Connection) connectionField.get(sqLite);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to get the connection from SQLite", e);
        }
    }
}
