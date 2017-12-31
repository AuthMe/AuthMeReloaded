package fr.xephi.authme.command.executable.authme.debug;

import ch.jalu.configme.properties.Property;
import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.xephi.authme.command.executable.authme.debug.DebugSectionUtils.castToTypeOrNull;
import static fr.xephi.authme.command.executable.authme.debug.DebugSectionUtils.unwrapSourceFromCacheDataSource;
import static fr.xephi.authme.data.auth.PlayerAuth.DB_EMAIL_DEFAULT;
import static fr.xephi.authme.data.auth.PlayerAuth.DB_LAST_IP_DEFAULT;
import static fr.xephi.authme.data.auth.PlayerAuth.DB_LAST_LOGIN_DEFAULT;
import static fr.xephi.authme.datasource.SqlDataSourceUtils.getColumnDefaultValue;
import static fr.xephi.authme.datasource.SqlDataSourceUtils.isNotNullColumn;
import static java.lang.String.format;

/**
 * Convenience command to add or remove the default value of a column and its nullable status
 * in the MySQL data source.
 */
class MySqlDefaultChanger implements DebugSection {

    private static final String NOT_NULL_SUFFIX = ChatColor.DARK_AQUA + "@" + ChatColor.RESET;
    private static final String DEFAULT_VALUE_SUFFIX = ChatColor.GOLD + "#" + ChatColor.RESET;

    @Inject
    private Settings settings;

    @Inject
    private DataSource dataSource;

    private MySQL mySql;

    @PostConstruct
    void setMySqlField() {
        this.mySql = castToTypeOrNull(unwrapSourceFromCacheDataSource(this.dataSource), MySQL.class);
    }

    @Override
    public String getName() {
        return "mysqldef";
    }

    @Override
    public String getDescription() {
        return "Add or remove the default value of MySQL columns";
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
        if (operation == Operation.DETAILS) {
            showColumnDetails(sender);
        } else if (operation == null || column == null) {
            displayUsageHints(sender);
        } else {
            sender.sendMessage(ChatColor.BLUE + "[AuthMe] MySQL change '" + column + "'");
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

    private void showColumnDetails(CommandSender sender) {
        sender.sendMessage(ChatColor.BLUE + "MySQL column details");
        final String tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        try (Connection con = getConnection(mySql)) {
            final DatabaseMetaData metaData = con.getMetaData();
            for (Columns col : Columns.values()) {
                String columnName = settings.getProperty(col.getColumnNameProperty());
                String isNullText = isNotNullColumn(metaData, tableName, columnName) ? "NOT NULL" : "nullable";
                Object defaultValue = getColumnDefaultValue(metaData, tableName, columnName);
                String defaultText = defaultValue == null ? "no default" : "default: '" + defaultValue + "'";
                sender.sendMessage(formatColumnWithMetadata(col, metaData, tableName)
                    + " (" + columnName + "): " + isNullText + ", " + defaultText);
            }
        } catch (SQLException e) {
            ConsoleLogger.logException("Failed while showing column details:", e);
            sender.sendMessage("Failed while showing column details. See log for info");
        }

    }

    /**
     * Displays sample commands and the list of columns that can be changed.
     *
     * @param sender the sender issuing the command
     */
    private void displayUsageHints(CommandSender sender) {
        sender.sendMessage(ChatColor.BLUE + "MySQL column changer");
        sender.sendMessage("Adds or removes a NOT NULL constraint for a column.");
        sender.sendMessage("Examples: add a NOT NULL constraint with");
        sender.sendMessage(" /authme debug mysqldef add <column>");
        sender.sendMessage("Remove one with /authme debug mysqldef remove <column>");

        sender.sendMessage("Available columns: " + constructColumnListWithMetadata());
        sender.sendMessage(" " + NOT_NULL_SUFFIX + ": not-null, " + DEFAULT_VALUE_SUFFIX
            + ": has default. See /authme debug mysqldef details");
    }

    /**
     * @return list of {@link Columns} we can toggle with suffixes indicating their NOT NULL and default value status
     */
    private String constructColumnListWithMetadata() {
        try (Connection con = getConnection(mySql)) {
            final DatabaseMetaData metaData = con.getMetaData();
            final String tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);

            List<String> formattedColumns = new ArrayList<>(Columns.values().length);
            for (Columns col : Columns.values()) {
                formattedColumns.add(formatColumnWithMetadata(col, metaData, tableName));
            }
            return String.join(ChatColor.RESET + ", ", formattedColumns);
        } catch (SQLException e) {
            ConsoleLogger.logException("Failed to construct column list:", e);
            return ChatColor.RED + "An error occurred! Please see the console for details.";
        }
    }

    private String formatColumnWithMetadata(Columns column, DatabaseMetaData metaData,
                                            String tableName) throws SQLException {
        String columnName = settings.getProperty(column.getColumnNameProperty());
        boolean isNotNull = isNotNullColumn(metaData, tableName, columnName);
        boolean hasDefaultValue = getColumnDefaultValue(metaData, tableName, columnName) != null;
        return column.name()
            + (isNotNull ? NOT_NULL_SUFFIX : "")
            + (hasDefaultValue ? DEFAULT_VALUE_SUFFIX : "");
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
            Method method = MySQL.class.getSuperclass().getDeclaredMethod("getConnection");
            method.setAccessible(true);
            return (Connection) method.invoke(mySql);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException("Could not get MySQL connection", e);
        }
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
        ADD, REMOVE, DETAILS
    }

    /** MySQL columns which can be toggled between being NOT NULL and allowing NULL values. */
    enum Columns {

        LASTLOGIN(DatabaseSettings.MYSQL_COL_LASTLOGIN,
            "BIGINT", "BIGINT NOT NULL DEFAULT 0", DB_LAST_LOGIN_DEFAULT),

        LASTIP(DatabaseSettings.MYSQL_COL_LAST_IP,
            "VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin",
            "VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT '127.0.0.1'",
            DB_LAST_IP_DEFAULT),

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
