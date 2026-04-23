package fr.xephi.authme.datasource;

import fr.xephi.authme.settings.Settings;

import java.sql.Connection;

/**
 * Resource closing test for {@link SQLite}.
 */
class SQLiteResourceClosingTest extends AbstractSqlDataSourceResourceClosingTest {

    @Override
    protected DataSource createDataSource(Settings settings, Connection connection) {
        return new SQLite(settings, null, connection);
    }
}
