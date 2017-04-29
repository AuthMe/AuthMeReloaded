package fr.xephi.authme.datasource;

import fr.xephi.authme.ConsoleLogger;

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
}
