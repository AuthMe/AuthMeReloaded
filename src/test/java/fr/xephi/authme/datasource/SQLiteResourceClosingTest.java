package fr.xephi.authme.datasource;

import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.NewSetting;

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
    protected DataSource createDataSource(NewSetting settings, Connection connection) throws Exception {
        return new SQLite(settings, connection);
    }

}
