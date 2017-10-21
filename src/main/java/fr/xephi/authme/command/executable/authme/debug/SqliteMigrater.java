package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.permission.DebugSectionPermissions;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.util.RandomStringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static fr.xephi.authme.command.executable.authme.debug.DebugSectionUtils.castToTypeOrNull;
import static fr.xephi.authme.command.executable.authme.debug.DebugSectionUtils.unwrapSourceFromCacheDataSource;
import static org.bukkit.ChatColor.BOLD;
import static org.bukkit.ChatColor.GOLD;

/**
 * Performs a migration on the SQLite data source if necessary.
 */
class SqliteMigrater implements DebugSection {

    @Inject
    private DataSource dataSource;

    @Inject
    private Settings settings;

    private SQLite sqLite;

    private String confirmationCode;

    @PostConstruct
    void setSqLiteField() {
        this.sqLite = castToTypeOrNull(unwrapSourceFromCacheDataSource(this.dataSource), SQLite.class);
    }

    @Override
    public String getName() {
        return "migratesqlite";
    }

    @Override
    public String getDescription() {
        return "Migrates the SQLite database";
    }

    // A migration can be forced even if SQLite says it doesn't need a migration by adding "force" as second argument
    @Override
    public void execute(CommandSender sender, List<String> arguments) {
        if (sqLite == null) {
            sender.sendMessage("This command migrates SQLite. You are currently not using a SQLite database.");
            return;
        }

        if (!isMigrationRequired() && !isMigrationForced(arguments)) {
            sender.sendMessage("Good news! No migration is required of your database");
        } else if (checkConfirmationCodeAndInformSenderOnMismatch(sender, arguments)) {
            final String tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
            final Columns columns = new Columns(settings);
            try {
                recreateDatabaseWithNewDefinitions(tableName, columns);
                sender.sendMessage(ChatColor.GREEN + "Successfully migrated your SQLite database!");
            } catch (SQLException e) {
                ConsoleLogger.logException("Failed to migrate SQLite database", e);
                sender.sendMessage(ChatColor.RED
                    + "An error occurred during SQLite migration. Please check the logs!");
            }
        }
    }

    private boolean checkConfirmationCodeAndInformSenderOnMismatch(CommandSender sender, List<String> arguments) {
        boolean isMatch = !arguments.isEmpty() && arguments.get(0).equalsIgnoreCase(confirmationCode);
        if (isMatch) {
            confirmationCode = null;
            return true;
        } else {
            confirmationCode = RandomStringUtils.generate(4).toUpperCase();
            sender.sendMessage(new String[]{
                BOLD.toString() + GOLD + "Please create a backup of your SQLite database before running this command!",
                "Either copy your DB file or run /authme backup. Afterwards,",
                String.format("run '/authme debug %s %s' to perform the migration. "
                    + "The code confirms that you've made a backup!", getName(), confirmationCode)
            });
            return false;
        }
    }

    @Override
    public PermissionNode getRequiredPermission() {
        return DebugSectionPermissions.MIGRATE_SQLITE;
    }

    private boolean isMigrationRequired() {
        Connection connection = getConnection(sqLite);
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            return sqLite.isMigrationRequired(metaData);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not check if SQLite migration is required", e);
        }
    }

    private static boolean isMigrationForced(List<String> arguments) {
        return arguments.size() >= 2 && "force".equals(arguments.get(1));
    }

    // Cannot rename or remove a column from SQLite, so we have to rename the table and create an updated one
    // cf. https://stackoverflow.com/questions/805363/how-do-i-rename-a-column-in-a-sqlite-database-table
    private void recreateDatabaseWithNewDefinitions(String tableName, Columns col) throws SQLException {
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
            int insertedEntries = st.executeUpdate(replaceColumnVariables(copySql, tableName, col));
            ConsoleLogger.info("Copied over " + insertedEntries + " from the old table to the new one");

            st.execute("DROP TABLE " + tempTable + ";");
        }
    }

    private String replaceColumnVariables(String sql, String tableName, Columns col) {
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
