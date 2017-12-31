package fr.xephi.authme.datasource;

import fr.xephi.authme.ConsoleLogger;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utilities for SQL data sources.
 */
public final class SqlDataSourceUtils {

    private SqlDataSourceUtils() {
    }

    /**
     * Logs a SQL exception.
     *
     * @param e the exception to log
     */
    public static void logSqlException(SQLException e) {
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
    public static Long getNullableLong(ResultSet rs, String columnName) throws SQLException {
        long longValue = rs.getLong(columnName);
        return rs.wasNull() ? null : longValue;
    }

    /**
     * Returns whether the given column has a NOT NULL constraint.
     *
     * @param metaData the database meta data
     * @param tableName the name of the table in which the column is
     * @param columnName the name of the column to check
     * @return true if the column is NOT NULL, false otherwise
     * @throws SQLException :)
     */
    public static boolean isNotNullColumn(DatabaseMetaData metaData, String tableName,
                                          String columnName) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            if (!rs.next()) {
                throw new IllegalStateException("Did not find meta data for column '"
                    + columnName + "' while checking for not-null constraint");
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
     * Returns the default value of a column (as per its SQL definition).
     *
     * @param metaData the database meta data
     * @param tableName the name of the table in which the column is
     * @param columnName the name of the column to check
     * @return the default value of the column (may be null)
     * @throws SQLException :)
     */
    public static Object getColumnDefaultValue(DatabaseMetaData metaData, String tableName,
                                               String columnName) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            if (!rs.next()) {
                throw new IllegalStateException("Did not find meta data for column '"
                    + columnName + "' while checking its default value");
            }
            return rs.getObject("COLUMN_DEF");
        }
    }

    public static boolean isColumnMissing(DatabaseMetaData metaData, String columnName, String tableName) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            return !rs.next();
        }
    }
}
