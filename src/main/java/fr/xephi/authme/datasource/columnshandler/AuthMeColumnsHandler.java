package fr.xephi.authme.datasource.columnshandler;

import ch.jalu.datasourcecolumns.data.DataSourceValue;
import ch.jalu.datasourcecolumns.data.DataSourceValues;
import ch.jalu.datasourcecolumns.data.UpdateValues;
import ch.jalu.datasourcecolumns.predicate.Predicate;
import ch.jalu.datasourcecolumns.sqlimplementation.PredicateSqlGenerator;
import ch.jalu.datasourcecolumns.sqlimplementation.PreparedStatementGenerator;
import ch.jalu.datasourcecolumns.sqlimplementation.ResultSetValueRetriever;
import ch.jalu.datasourcecolumns.sqlimplementation.SqlColumnsHandler;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static fr.xephi.authme.datasource.SqlDataSourceUtils.logSqlException;

/**
 * Wrapper of {@link SqlColumnsHandler} for the AuthMe data table.
 * Wraps exceptions and provides better support for operations based on a {@link PlayerAuth} object.
 */
public final class AuthMeColumnsHandler {

    private final SqlColumnsHandler<ColumnContext, String> internalHandler;

    private AuthMeColumnsHandler(SqlColumnsHandler<ColumnContext, String> internalHandler) {
        this.internalHandler = internalHandler;
    }

    /**
     * Creates a column handler for SQLite.
     *
     * @param connection the connection to the database
     * @param settings plugin settings
     * @return created column handler
     */
    public static AuthMeColumnsHandler createForSqlite(Connection connection, Settings settings) {
        ColumnContext columnContext = new ColumnContext(settings, false);
        String tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        String nameColumn = settings.getProperty(DatabaseSettings.MYSQL_COL_NAME);

        SqlColumnsHandler<ColumnContext, String> sqlColHandler = new SqlColumnsHandler<>(
            PreparedStatementGenerator.fromConnection(connection), columnContext, tableName, nameColumn,
            new ResultSetValueRetriever<>(columnContext), new PredicateSqlGenerator<>(columnContext, true));
        return new AuthMeColumnsHandler(sqlColHandler);
    }

    /**
     * Creates a column handler for MySQL.
     *
     * @param connectionSupplier supplier of connections from the connection pool
     * @param settings plugin settings
     * @return created column handler
     */
    public static AuthMeColumnsHandler createForMySql(ConnectionSupplier connectionSupplier, Settings settings) {
        ColumnContext columnContext = new ColumnContext(settings, true);
        String tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
        String nameColumn = settings.getProperty(DatabaseSettings.MYSQL_COL_NAME);

        SqlColumnsHandler<ColumnContext, String> sqlColHandler = new SqlColumnsHandler<>(
            new MySqlPreparedStatementGenerator(connectionSupplier), columnContext, tableName, nameColumn,
            new ResultSetValueRetriever<>(columnContext), new PredicateSqlGenerator<>(columnContext));
        return new AuthMeColumnsHandler(sqlColHandler);
    }

    /**
     * Changes a column from a specific row to the given value.
     *
     * @param name name of the account to modify
     * @param column the column to modify
     * @param value the value to set the column to
     * @param <T> the column type
     * @return true upon success, false otherwise
     */
    public <T> boolean update(String name, DataSourceColumn<T> column, T value) {
        try {
            return internalHandler.update(name, column, value);
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    /**
     * Updates a row to have the values as retrieved from the PlayerAuth object.
     *
     * @param auth the player auth object to modify and to get values from
     * @param columns the columns to update in the row
     * @return true upon success, false otherwise
     */
    public boolean update(PlayerAuth auth, PlayerAuthColumn<?>... columns) {
        try {
            return internalHandler.update(auth.getNickname(), auth, columns);
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    /**
     * Updates a row to have the given values.
     *
     * @param name the name of the account to modify
     * @param updateValues the values to set on the row
     * @return true upon success, false otherwise
     */
    public boolean update(String name, UpdateValues<ColumnContext> updateValues) {
        try {
            return internalHandler.update(name.toLowerCase(), updateValues);
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    /**
     * Sets the given value to the provided column for all rows which match the predicate.
     *
     * @param predicate the predicate to filter rows by
     * @param column the column to modify on the matched rows
     * @param value the new value to set
     * @param <T> the column type
     * @return number of modified rows
     */
    public <T> int update(Predicate<ColumnContext> predicate, DataSourceColumn<T> column, T value) {
        try {
            return internalHandler.update(predicate, column, value);
        } catch (SQLException e) {
            logSqlException(e);
            return 0;
        }
    }

    /**
     * Retrieves the given column from a given row.
     *
     * @param name the account name to look up
     * @param column the column whose value should be retrieved
     * @param <T> the column type
     * @return the result of the lookup
     * @throws SQLException .
     */
    public <T> DataSourceValue<T> retrieve(String name, DataSourceColumn<T> column) throws SQLException {
        return internalHandler.retrieve(name.toLowerCase(), column);
    }

    /**
     * Retrieves multiple values from a given row.
     *
     * @param name the account name to look up
     * @param columns the columns to retrieve
     * @return map-like object with the requested values
     * @throws SQLException .
     */
    public DataSourceValues retrieve(String name, DataSourceColumn<?>... columns) throws SQLException {
        return internalHandler.retrieve(name.toLowerCase(), columns);
    }

    /**
     * Retrieves a column's value for all rows that satisfy the given predicate.
     *
     * @param predicate the predicate to fulfill
     * @param column the column to retrieve from the matching rows
     * @param <T> the column's value type
     * @return the values of the matching rows
     * @throws SQLException .
     */
    public <T> List<T> retrieve(Predicate<ColumnContext> predicate, DataSourceColumn<T> column) throws SQLException {
        return internalHandler.retrieve(predicate, column);
    }

    /**
     * Inserts the given values into a new row, as taken from the player auth.
     *
     * @param auth the player auth to get values from
     * @param columns the columns to insert
     * @return true upon success, false otherwise
     */
    public boolean insert(PlayerAuth auth, PlayerAuthColumn<?>... columns) {
        try {
            return internalHandler.insert(auth, columns);
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    /**
     * Returns the number of rows that match the provided predicate.
     *
     * @param predicate the predicate to test the rows for
     * @return number of rows fulfilling the predicate
     */
    public int count(Predicate<ColumnContext> predicate) {
        try {
            return internalHandler.count(predicate);
        } catch (SQLException e) {
            logSqlException(e);
            return 0;
        }
    }
}
