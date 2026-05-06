package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.SqlConnectionSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base class for converters that read from an external plugin's MySQL/MariaDB table.
 * <p>
 * The source plugin is expected to share the same MySQL/MariaDB database as AuthMe.
 * AuthMe's existing HikariCP connection pool is reused — no separate connection is opened.
 * SQLite data sources are not supported by this converter base class.
 */
abstract class AbstractSqlPluginConverter implements Converter {

    private final DataSource dataSource;

    AbstractSqlPluginConverter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Returns a connection from AuthMe's existing HikariCP pool.
     *
     * @return an open connection
     * @throws SQLException if the data source does not support SQL connections (e.g. SQLite)
     */
    protected Connection openConnection() throws SQLException {
        if (dataSource instanceof SqlConnectionSource sqlSource) {
            return sqlSource.getConnection();
        }
        throw new SQLException(
            "This converter requires AuthMe to be configured with MySQL or MariaDB. "
                + "SQLite is not supported as a shared data source for plugin converters.");
    }
}
