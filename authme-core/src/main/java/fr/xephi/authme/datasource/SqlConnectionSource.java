package fr.xephi.authme.datasource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Implemented by SQL-backed data sources that can provide raw JDBC connections from their pool.
 * Used by converters that need to query a foreign table in the same database.
 */
public interface SqlConnectionSource {

    Connection getConnection() throws SQLException;
}
