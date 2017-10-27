package fr.xephi.authme.datasource;

import fr.xephi.authme.settings.Settings;

import java.lang.reflect.Method;
import java.sql.Connection;

/**
 * Resource closing test for {@link SQLite}.
 */
public class SQLiteResourceClosingTest extends AbstractSqlDataSourceResourceClosingTest {

    public SQLiteResourceClosingTest(Method method, String name) {
        super(method, name);
    }

    @Override
    protected DataSource createDataSource(Settings settings, Connection connection) throws Exception {
        return new SQLite(settings, null, connection);
    }

}
