package fr.xephi.authme.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.ConsoleLoggerTestInitializer;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link MySQL}.
 */
public class MySqlIntegrationTest extends AbstractDataSourceIntegrationTest {

    /** Mock of a settings instance. */
    private static NewSetting settings;
    /** SQL statement to execute before running a test. */
    private static String sqlInitialize;
    /** Connection to the H2 test database. */
    private HikariDataSource hikariSource;

    /**
     * Set up the settings mock to return specific values for database settings and load {@link #sqlInitialize}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BeforeClass
    public static void initializeSettings() throws IOException, ClassNotFoundException {
        // Check that we have an H2 driver
        Class.forName("org.h2.jdbcx.JdbcDataSource");

        settings = mock(NewSetting.class);
        when(settings.getProperty(any(Property.class))).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return ((Property) invocation.getArguments()[0]).getDefaultValue();
            }
        });
        set(DatabaseSettings.MYSQL_DATABASE, "h2_test");
        set(DatabaseSettings.MYSQL_TABLE, "authme");
        set(DatabaseSettings.MYSQL_COL_SALT, "salt");
        ConsoleLoggerTestInitializer.setupLogger();

        Path sqlInitFile = TestHelper.getJarPath("/datasource-integration/sql-initialize.sql");
        sqlInitialize = new String(Files.readAllBytes(sqlInitFile));
    }

    @Before
    public void initializeConnectionAndTable() throws SQLException {
        silentClose(hikariSource);
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
        config.setConnectionTestQuery("VALUES 1");
        config.addDataSourceProperty("URL", "jdbc:h2:mem:test");
        config.addDataSourceProperty("user", "sa");
        config.addDataSourceProperty("password", "sa");
        HikariDataSource ds = new HikariDataSource(config);
        Connection connection = ds.getConnection();

        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS authme");
            st.execute(sqlInitialize);
        }
        hikariSource = ds;
    }

    @Override
    protected DataSource getDataSource() {
        return new MySQL(settings, hikariSource);
    }

    private static <T> void set(Property<T> property, T value) {
        when(settings.getProperty(property)).thenReturn(value);
    }

    private static void silentClose(HikariDataSource con) {
        if (con != null && !con.isClosed()) {
            con.close();
        }
    }

}
