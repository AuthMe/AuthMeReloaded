package fr.xephi.authme.datasource;

import fr.xephi.authme.datasource.AbstractResourceClosingTest;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.SQLite;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.Settings;

import java.lang.reflect.Method;
import java.sql.Connection;

/**
 * Resource closing test for {@link SQLite}.
 */
public class SQLiteResourceClosingTest extends AbstractResourceClosingTest {

    public SQLiteResourceClosingTest(Method method, String name, HashAlgorithm algorithm) {
        super(method, name, algorithm);
    }

    @Override
    protected DataSource createDataSource(Settings settings, Connection connection) throws Exception {
        return new SQLite(settings, connection);
    }

}
