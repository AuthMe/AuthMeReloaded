package fr.xephi.authme.datasource.converter;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Base class for converters that read from an external plugin's MySQL/MariaDB table.
 * <p>
 * The source database is assumed to share the same host, port, database name, and credentials
 * as configured in AuthMe's {@code config.yml}. SQLite source databases are not supported.
 */
abstract class AbstractSqlPluginConverter implements Converter {

    private final DataSource dataSource;
    private final Settings settings;

    AbstractSqlPluginConverter(Settings settings, DataSource dataSource) {
        this.settings = settings;
        this.dataSource = dataSource;
    }

    protected DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Opens a JDBC connection to the database configured in AuthMe's settings.
     *
     * @return an open connection
     * @throws SQLException if the connection cannot be established
     */
    protected Connection openConnection() throws SQLException {
        String host = settings.getProperty(DatabaseSettings.MYSQL_HOST);
        String port = settings.getProperty(DatabaseSettings.MYSQL_PORT);
        String database = settings.getProperty(DatabaseSettings.MYSQL_DATABASE);
        String user = settings.getProperty(DatabaseSettings.MYSQL_USERNAME);
        String pass = settings.getProperty(DatabaseSettings.MYSQL_PASSWORD);
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
            + "?useUnicode=true&characterEncoding=utf-8";
        return DriverManager.getConnection(url, user, pass);
    }
}
