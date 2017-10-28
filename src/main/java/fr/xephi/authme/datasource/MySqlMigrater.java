package fr.xephi.authme.datasource;

import fr.xephi.authme.ConsoleLogger;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * Performs migrations on the MySQL data source if necessary.
 */
final class MySqlMigrater {

    private MySqlMigrater() {
    }

    /**
     * Changes the last IP column to be nullable if it has a NOT NULL constraint without a default value.
     * Background: Before 5.2, the last IP column was initialized to be {@code NOT NULL} without a default.
     * With the introduction of a registration IP column we no longer want to set the last IP column on registration.
     *
     * @param st Statement object to the database
     * @param metaData column metadata for the table
     * @param tableName the MySQL table's name
     * @param col the column names configuration
     */
    static void migrateLastIpColumn(Statement st, DatabaseMetaData metaData,
                                    String tableName, Columns col) throws SQLException {
        final boolean isNotNullWithoutDefault = SqlDataSourceUtils.isNotNullColumn(metaData, tableName, col.LAST_IP)
            && SqlDataSourceUtils.getColumnDefaultValue(metaData, tableName, col.LAST_IP) == null;

        if (isNotNullWithoutDefault) {
            String sql = String.format("ALTER TABLE %s MODIFY %s VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin",
                tableName, col.LAST_IP);
            st.execute(sql);
            ConsoleLogger.info("Changed last login column to allow NULL values. Please verify the registration feature "
                + "if you are hooking into a forum.");
        }
    }

    /**
     * Checks if the last login column has a type that needs to be migrated.
     *
     * @param st Statement object to the database
     * @param metaData column metadata for the table
     * @param tableName the MySQL table's name
     * @param col the column names configuration
     */
    static void migrateLastLoginColumn(Statement st, DatabaseMetaData metaData,
                                       String tableName, Columns col) throws SQLException {
        final int columnType;
        try (ResultSet rs = metaData.getColumns(null, null, tableName, col.LAST_LOGIN)) {
            if (!rs.next()) {
                ConsoleLogger.warning("Could not get LAST_LOGIN meta data. This should never happen!");
                return;
            }
            columnType = rs.getInt("DATA_TYPE");
        }

        if (columnType == Types.TIMESTAMP) {
            migrateLastLoginColumnFromTimestamp(st, tableName, col);
        } else if (columnType == Types.INTEGER) {
            migrateLastLoginColumnFromInt(st, tableName, col);
        }
    }

    /**
     * Performs conversion of lastlogin column from timestamp type to bigint.
     *
     * @param st Statement object to the database
     * @param tableName the table name
     * @param col the column names configuration
     * @see <a href="https://github.com/AuthMe/AuthMeReloaded/issues/477">#477</a>
     */
    private static void migrateLastLoginColumnFromTimestamp(Statement st, String tableName,
                                                            Columns col) throws SQLException {
        ConsoleLogger.info("Migrating lastlogin column from timestamp to bigint");
        final String lastLoginOld = col.LAST_LOGIN + "_old";

        // Rename lastlogin to lastlogin_old
        String sql = String.format("ALTER TABLE %s CHANGE COLUMN %s %s BIGINT",
            tableName, col.LAST_LOGIN, lastLoginOld);
        st.execute(sql);

        // Create lastlogin column
        sql = String.format("ALTER TABLE %s ADD COLUMN %s "
                + "BIGINT NOT NULL DEFAULT 0 AFTER %s",
            tableName, col.LAST_LOGIN, col.LAST_IP);
        st.execute(sql);

        // Set values of lastlogin based on lastlogin_old
        sql = String.format("UPDATE %s SET %s = UNIX_TIMESTAMP(%s) * 1000",
            tableName, col.LAST_LOGIN, lastLoginOld);
        st.execute(sql);

        // Drop lastlogin_old
        sql = String.format("ALTER TABLE %s DROP COLUMN %s",
            tableName, lastLoginOld);
        st.execute(sql);
        ConsoleLogger.info("Finished migration of lastlogin (timestamp to bigint)");
    }

    /**
     * Performs conversion of lastlogin column from int to bigint.
     *
     * @param st Statement object to the database
     * @param tableName the table name
     * @param col the column names configuration
     * @see <a href="https://github.com/AuthMe/AuthMeReloaded/issues/887">
     *      #887: Migrate lastlogin column from int32 to bigint</a>
     */
    private static void migrateLastLoginColumnFromInt(Statement st, String tableName, Columns col) throws SQLException {
        // Change from int to bigint
        ConsoleLogger.info("Migrating lastlogin column from int to bigint");
        String sql = String.format("ALTER TABLE %s MODIFY %s BIGINT;", tableName, col.LAST_LOGIN);
        st.execute(sql);

        // Migrate timestamps in seconds format to milliseconds format if they are plausible
        int rangeStart = 1262304000; // timestamp for 2010-01-01
        int rangeEnd = 1514678400;   // timestamp for 2017-12-31
        sql = String.format("UPDATE %s SET %s = %s * 1000 WHERE %s > %d AND %s < %d;",
            tableName, col.LAST_LOGIN, col.LAST_LOGIN, col.LAST_LOGIN, rangeStart, col.LAST_LOGIN, rangeEnd);
        int changedRows = st.executeUpdate(sql);

        ConsoleLogger.warning("You may have entries with invalid timestamps. Please check your data "
            + "before purging. " + changedRows + " rows were migrated from seconds to milliseconds.");
    }

    /**
     * Creates the column for registration date and sets all entries to the current timestamp.
     * We do so in order to avoid issues with purging, where entries with 0 / NULL might get
     * purged immediately on startup otherwise.
     *
     * @param st Statement object to the database
     * @param tableName the table name
     * @param col the column names configuration
     */
    static void addRegistrationDateColumn(Statement st, String tableName, Columns col) throws SQLException {
        st.executeUpdate("ALTER TABLE " + tableName
            + " ADD COLUMN " + col.REGISTRATION_DATE + " BIGINT NOT NULL DEFAULT 0;");

        // Use the timestamp from Java to avoid timezone issues in case JVM and database are out of sync
        long currentTimestamp = System.currentTimeMillis();
        int updatedRows = st.executeUpdate(String.format("UPDATE %s SET %s = %d;",
            tableName, col.REGISTRATION_DATE, currentTimestamp));
        ConsoleLogger.info("Created column '" + col.REGISTRATION_DATE + "' and set the current timestamp, "
            + currentTimestamp + ", to all " + updatedRows + " rows");
    }
}
