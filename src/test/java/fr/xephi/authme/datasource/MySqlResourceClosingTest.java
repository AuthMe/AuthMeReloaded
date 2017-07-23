package fr.xephi.authme.datasource;

import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.datasource.mysqlextensions.MySqlExtension;
import fr.xephi.authme.datasource.mysqlextensions.MySqlExtensionsFactory;
import fr.xephi.authme.settings.Settings;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Resource closing test for {@link MySQL}.
 */
public class MySqlResourceClosingTest extends AbstractSqlDataSourceResourceClosingTest {

    public MySqlResourceClosingTest(Method method, String name) {
        super(method, name);
    }

    @Override
    protected DataSource createDataSource(Settings settings, Connection connection) throws Exception {
        HikariDataSource hikariDataSource = mock(HikariDataSource.class);
        given(hikariDataSource.getConnection()).willReturn(connection);
        MySqlExtensionsFactory extensionsFactory = mock(MySqlExtensionsFactory.class);
        given(extensionsFactory.buildExtension(any(Columns.class))).willReturn(mock(MySqlExtension.class));
        return new MySQL(settings, hikariDataSource, extensionsFactory);
    }

}
