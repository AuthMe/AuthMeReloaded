package fr.xephi.authme.datasource;

import com.github.authme.configme.properties.Property;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.AbstractDataSourceIntegrationTest;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.MySQL;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import org.junit.After;
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
    private static Settings settings;
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

        settings = mock(Settings.class);
        when(settings.getProperty(any(Property.class))).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return ((Property) invocation.getArguments()[0]).getDefaultValue();
            }
        });
        set(DatabaseSettings.MYSQL_DATABASE, "h2_test");
        set(DatabaseSettings.MYSQL_TABLE, "authme");
        TestHelper.setRealLogger();

        Path sqlInitFile = TestHelper.getJarPath(TestHelper.PROJECT_ROOT + "datasource/sql-initialize.sql");
        sqlInitialize = new String(Files.readAllBytes(sqlInitFile));
    }

    @Before
    public void initializeConnectionAndTable() throws SQLException {
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

    @After
    public void closeConnection() {
        silentClose(hikariSource);
    }

    @Override
    protected DataSource getDataSource(String saltColumn) {
        when(settings.getProperty(DatabaseSettings.MYSQL_COL_SALT)).thenReturn(saltColumn);
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
