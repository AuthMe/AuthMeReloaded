package fr.xephi.authme.datasource;

import fr.xephi.authme.ConsoleLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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


    // We use overloaded close() methods instead of one close(AutoCloseable) method in order to limit the
    // checked exceptions to SQLException, which is the only checked exception these classes throw.

    /**
     * Closes a {@link ResultSet} safely.
     *
     * @param rs the result set to close
     */
    static void close(ResultSet rs) {
        try {
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }
        } catch (SQLException e) {
            ConsoleLogger.logException("Could not close ResultSet", e);
        }
    }

    /**
     * Closes a {@link Statement} safely.
     *
     * @param st the statement set to close
     */
    static void close(Statement st) {
        try {
            if (st != null && !st.isClosed()) {
                st.close();
            }
        } catch (SQLException e) {
            ConsoleLogger.logException("Could not close Statement", e);
        }
    }

    /**
     * Closes a {@link Connection} safely.
     *
     * @param con the connection set to close
     */
    static void close(Connection con) {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException e) {
            ConsoleLogger.logException("Could not close Connection", e);
        }
    }
}
