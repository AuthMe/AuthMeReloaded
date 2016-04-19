package fr.xephi.authme.datasource;

import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.NewSetting;

import java.lang.reflect.Method;
import java.sql.Connection;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Resource closing test for {@link MySQL}.
 */
public class MySqlResourceClosingTest extends AbstractResourceClosingTest {

    public MySqlResourceClosingTest(Method method, String name, HashAlgorithm algorithm) {
        super(method, name, algorithm);
    }

    @Override
    protected DataSource createDataSource(NewSetting settings, Connection connection) throws Exception {
        HikariDataSource hikariDataSource = mock(HikariDataSource.class);
        given(hikariDataSource.getConnection()).willReturn(connection);
        return new MySQL(settings, hikariDataSource);
    }

}
