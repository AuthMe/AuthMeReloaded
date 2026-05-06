package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.datasource.DataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Base class for converters that read from an external plugin's SQLite database file.
 */
abstract class AbstractSqlitePluginConverter implements Converter {

    private final DataSource dataSource;

    AbstractSqlitePluginConverter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Opens a JDBC connection to the given SQLite database file.
     *
     * @param dbFile the SQLite database file
     * @return an open connection
     * @throws SQLException if the driver is unavailable or the connection cannot be established
     */
    protected Connection openConnection(File dbFile) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not available", e);
        }
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }
}
