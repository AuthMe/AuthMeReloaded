package fr.xephi.authme.datasource;

import fr.xephi.authme.ConsoleLogger;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utilities for SQL data sources.
 */
final class SqlDataSourceUtils {

    private SqlDataSourceUtils() {
    }

    /**
     * Logs a SQL exception.
     *
     * @param e the exception to log
     */
    static void logSqlException(SQLException e) {
        ConsoleLogger.logException("Error during SQL operation:", e);
    }

    /**
     * Returns the long value of a column, or null when appropriate. This method is necessary because
     * JDBC's {@link ResultSet#getLong} returns {@code 0} if the entry in the database is {@code null}.
     *
     * @param rs the result set to read from
     * @param columnName the name of the column to retrieve
     * @return the value (which may be null)
     * @throws SQLException :)
     */
    static Long getNullableLong(ResultSet rs, String columnName) throws SQLException {
        long longValue = rs.getLong(columnName);
        return rs.wasNull() ? null : longValue;
    }
}
