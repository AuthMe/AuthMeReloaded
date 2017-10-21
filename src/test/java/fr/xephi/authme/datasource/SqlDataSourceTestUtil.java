package fr.xephi.authme.datasource;

import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.datasource.mysqlextensions.MySqlExtension;
import fr.xephi.authme.datasource.mysqlextensions.MySqlExtensionsFactory;
import fr.xephi.authme.settings.Settings;

import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test util for the SQL data sources.
 */
public final class SqlDataSourceTestUtil {

    private SqlDataSourceTestUtil() {
    }

    public static MySQL createMySql(Settings settings, HikariDataSource hikariDataSource) {
        MySqlExtensionsFactory extensionsFactory = mock(MySqlExtensionsFactory.class);
        given(extensionsFactory.buildExtension(any())).willReturn(mock(MySqlExtension.class));
        return new MySQL(settings, hikariDataSource, extensionsFactory);
    }

    public static SQLite createSqlite(Settings settings, Connection connection) {
        return new SQLite(settings, connection) {
            // Override reload() so it doesn't run SQLite#connect, since we're given a specific Connection to use
            @Override
            public void reload() {
                try {
                    this.setup();
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    public static SQLite createSqliteAndInitialize(Settings settings, Connection connection) {
        SQLite sqLite = createSqlite(settings, connection);
        try {
            sqLite.setup();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return sqLite;
    }
}
