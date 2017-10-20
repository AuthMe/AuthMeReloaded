package fr.xephi.authme.command.executable.authme.debug;

import ch.jalu.configme.properties.Property;
import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.MySQL;
import fr.xephi.authme.permission.DebugSectionPermissions;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.xephi.authme.data.auth.PlayerAuth.DB_EMAIL_DEFAULT;
import static fr.xephi.authme.data.auth.PlayerAuth.DB_LAST_LOGIN_DEFAULT;
import static java.lang.String.format;

/**
 * Convenience command to add or remove the default value of a column and its nullable status
 * in the MySQL data source.
 */
class MySqlDefaultChanger implements DebugSection {

    @Inject
    private Settings settings;

    @Inject
    private DataSource dataSource;

    private MySQL mySql;

    @PostConstruct
    void setMySqlField() {
        DataSource dataSource = unwrapSourceFromCacheDataSource(this.dataSource);
        if (dataSource instanceof MySQL) {
            this.mySql = (MySQL) dataSource;
        }
    }

    @Override
    public String getName() {
        return "mysqldef";
    }

    @Override
    public String getDescription() {
        return "Add or remove the default value of a column for MySQL";
    }

    @Override
    public PermissionNode getRequiredPermission() {
        return DebugSectionPermissions.MYSQL_DEFAULT_CHANGER;
    }

    @Override
    public void execute(CommandSender sender, List<String> arguments) {
        if (mySql == null) {
            sender.sendMessage("Defaults can be changed for the MySQL data source only.");
            return;
        }

        Operation operation = matchToEnum(arguments, 0, Operation.class);
        Columns column = matchToEnum(arguments, 1, Columns.class);
        if (operation == null || column == null) {
            displayUsageHints(sender);
        } else {
            try (Connection con = getConnection(mySql)) {
                switch (operation) {
                    case ADD:
                        changeColumnToNotNullWithDefault(sender, column, con);
                        break;
                    case REMOVE:
                        removeNotNullAndDefault(sender, column, con);
                        break;
                    default:
                        throw new IllegalStateException("Unknown operation '" + operation + "'");
                }
            } catch (SQLException | IllegalStateException e) {
                ConsoleLogger.logException("Failed to perform MySQL default altering operation:", e);
            }
        }
    }

    /**
     * Adds a default value to the column definition and adds a {@code NOT NULL} constraint for
     * the specified column.
     *
     * @param sender the command sender initiation the action
     * @param column the column to modify
     * @param con connection to the database
     * @throws SQLException .
     */
    private void changeColumnToNotNullWithDefault(CommandSender sender, Columns column,
                                                  Connection con) throws SQLException {
        final String tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        final String columnName = settings.getProperty(column.getColumnNameProperty());

        // Replace NULLs with future default value
        String sql = format("UPDATE %s SET %s = ? WHERE %s IS NULL;", tableName, columnName, columnName);
        int updatedRows;
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setObject(1, column.getDefaultValue());
            updatedRows = pst.executeUpdate();
        }
        sender.sendMessage("Replaced NULLs with default value ('" + column.getDefaultValue()
            + "'), modifying " + updatedRows + " entries");

        // Change column definition to NOT NULL version
        try (Statement st = con.createStatement()) {
            st.execute(format("ALTER TABLE %s MODIFY %s %s", tableName, columnName, column.getNotNullDefinition()));
            sender.sendMessage("Changed column '" + columnName + "' to have NOT NULL constraint");
        }

        // Log success message
        ConsoleLogger.info("Changed MySQL column '" + columnName + "' to be NOT NULL, as initiated by '"
            + sender.getName() + "'");
    }

    /**
     * Removes the {@code NOT NULL} constraint of a column definition and replaces rows with the
     * default value to {@code NULL}.
     *
     * @param sender the command sender initiation the action
     * @param column the column to modify
     * @param con connection to the database
     * @throws SQLException .
     */
    private void removeNotNullAndDefault(CommandSender sender, Columns column, Connection con) throws SQLException {
        final String tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        final String columnName = settings.getProperty(column.getColumnNameProperty());

        // Change column definition to nullable version
        try (Statement st = con.createStatement()) {
            st.execute(format("ALTER TABLE %s MODIFY %s %s", tableName, columnName, column.getNullableDefinition()));
            sender.sendMessage("Changed column '" + columnName + "' to allow nulls");
        }

        // Replace old default value with NULL
        String sql = format("UPDATE %s SET %s = NULL WHERE %s = ?;", tableName, columnName, columnName);
        int updatedRows;
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setObject(1, column.getDefaultValue());
            updatedRows = pst.executeUpdate();
        }
        sender.sendMessage("Replaced default value ('" + column.getDefaultValue()
            + "') to be NULL, modifying " + updatedRows + " entries");

        // Log success message
        ConsoleLogger.info("Changed MySQL column '" + columnName + "' to allow NULL, as initiated by '"
            + sender.getName() + "'");
    }

    /**
     * Displays sample commands and the list of columns that can be changed.
     *
     * @param sender the sender issuing the command
     */
    private void displayUsageHints(CommandSender sender) {
        sender.sendMessage("Adds or removes a NOT NULL constraint for a column.");
        sender.sendMessage("  Only available for MySQL.");
        if (mySql == null) {
            sender.sendMessage("You are currently not using MySQL!");
            return;
        }

        sender.sendMessage("Examples: add a NOT NULL constraint with");
        sender.sendMessage(" /authme debug mysqldef add <column>");
        sender.sendMessage("Remove a NOT NULL constraint with");
        sender.sendMessage(" /authme debug mysqldef remove <column>");

        // Note ljacqu 20171015: Intentionally avoid green & red as to avoid suggesting that one state is good or bad
        sender.sendMessage("Available columns: " + constructColoredColumnList());
        sender.sendMessage(" where " + ChatColor.DARK_AQUA + "blue " + ChatColor.RESET
            + "is currently not-null, and " + ChatColor.GOLD + "gold " + ChatColor.RESET + "is null");
    }

    /**
     * @return list of {@link Columns} we can toggle, colored by their current not-null status
     */
    private String constructColoredColumnList() {
        try (Connection con = getConnection(mySql)) {
            final DatabaseMetaData metaData = con.getMetaData();
            final String tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);

            List<String> formattedColumns = new ArrayList<>(Columns.values().length);
            for (Columns col : Columns.values()) {
                String columnName = settings.getProperty(col.getColumnNameProperty());
                boolean isNotNull = isNotNullColumn(metaData, tableName, columnName);
                String formattedColumn = (isNotNull ? ChatColor.DARK_AQUA : ChatColor.GOLD) + col.name().toLowerCase();
                formattedColumns.add(formattedColumn);
            }
            return String.join(ChatColor.RESET + ", ", formattedColumns);
        } catch (SQLException e) {
            ConsoleLogger.logException("Failed to construct column list:", e);
            return ChatColor.RED + "An error occurred! Please see the console for details.";
        }
    }

    private boolean isNotNullColumn(DatabaseMetaData metaData, String tableName,
                                    String columnName) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            if (!rs.next()) {
                throw new IllegalStateException("Did not find meta data for column '" + columnName
                    + "' while migrating not-null columns (this should never happen!)");
            }

            int nullableCode = rs.getInt("NULLABLE");
            if (nullableCode == DatabaseMetaData.columnNoNulls) {
                return true;
            } else if (nullableCode == DatabaseMetaData.columnNullableUnknown) {
                ConsoleLogger.warning("Unknown nullable status for column '" + columnName + "'");
            }
        }
        return false;
    }

    /**
     * Gets the Connection object from the MySQL data source.
     *
     * @param mySql the MySQL data source to get the connection from
     * @return the connection
     */
    @VisibleForTesting
    Connection getConnection(MySQL mySql) {
        try {
            Method method = MySQL.class.getDeclaredMethod("getConnection");
            method.setAccessible(true);
            return (Connection) method.invoke(mySql);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException("Could not get MySQL connection", e);
        }
    }

    /**
     * Unwraps the "cache data source" and returns the underlying source. Returns the
     * same as the input argument otherwise.
     *
     * @param dataSource the data source to unwrap if applicable
     * @return the non-cache data source
     */
    @VisibleForTesting
    static DataSource unwrapSourceFromCacheDataSource(DataSource dataSource) {
        if (dataSource instanceof CacheDataSource) {
            try {
                Field source = CacheDataSource.class.getDeclaredField("source");
                source.setAccessible(true);
                return (DataSource) source.get(dataSource);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                ConsoleLogger.logException("Could not get source of CacheDataSource:", e);
                return null;
            }
        }
        return dataSource;
    }

    private static <E extends Enum<E>> E matchToEnum(List<String> arguments, int index, Class<E> clazz) {
        if (arguments.size() <= index) {
            return null;
        }
        String str = arguments.get(index);
        return Arrays.stream(clazz.getEnumConstants())
            .filter(e -> e.name().equalsIgnoreCase(str))
            .findFirst().orElse(null);
    }

    private enum Operation {
        ADD, REMOVE
    }

    /** MySQL columns which can be toggled between being NOT NULL and allowing NULL values. */
    enum Columns {

        LASTLOGIN(DatabaseSettings.MYSQL_COL_LASTLOGIN,
            "BIGINT", "BIGINT NOT NULL DEFAULT 0", DB_LAST_LOGIN_DEFAULT),

        EMAIL(DatabaseSettings.MYSQL_COL_EMAIL,
            "VARCHAR(255)", "VARCHAR(255) NOT NULL DEFAULT 'your@email.com'", DB_EMAIL_DEFAULT);

        private final Property<String> columnNameProperty;
        private final String nullableDefinition;
        private final String notNullDefinition;
        private final Object defaultValue;

        Columns(Property<String> columnNameProperty, String nullableDefinition,
                String notNullDefinition, Object defaultValue) {
            this.columnNameProperty = columnNameProperty;
            this.nullableDefinition = nullableDefinition;
            this.notNullDefinition = notNullDefinition;
            this.defaultValue = defaultValue;
        }

        /** @return property defining the column name in the database */
        Property<String> getColumnNameProperty() {
            return columnNameProperty;
        }

        /** @return SQL definition of the column allowing NULL values */
        String getNullableDefinition() {
            return nullableDefinition;
        }

        /** @return SQL definition of the column with a NOT NULL constraint */
        String getNotNullDefinition() {
            return notNullDefinition;
        }

        /** @return the default value used in {@link #notNullDefinition} */
        Object getDefaultValue() {
            return defaultValue;
        }
    }
}
