package fr.xephi.authme.datasource;

import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.datasource.mysqlextensions.MySqlExtension;
import fr.xephi.authme.datasource.mysqlextensions.MySqlExtensionsFactory;
import fr.xephi.authme.settings.Settings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test util for the MySQL data source.
 */
public final class MySqlTestUtil {

    private MySqlTestUtil() {
    }

    public static MySQL createMySql(Settings settings, HikariDataSource hikariDataSource) {
        MySqlExtensionsFactory extensionsFactory = mock(MySqlExtensionsFactory.class);
        given(extensionsFactory.buildExtension(any())).willReturn(mock(MySqlExtension.class));
        return createMySql(settings, hikariDataSource, extensionsFactory);
    }

    public static MySQL createMySql(Settings settings, HikariDataSource hikariDataSource,
                                    MySqlExtensionsFactory extensionsFactory) {
        return new MySQL(settings, hikariDataSource, extensionsFactory);
    }
}
