package fr.xephi.authme.datasource;

import ch.jalu.configme.properties.Property;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link PostgreSqlDataSource}.
 */
class PostgreSqlIntegrationTest extends AbstractDataSourceIntegrationTest {

    /** Mock of a settings instance. */
    private static Settings settings;
    /** SQL statement to execute before running a test. */
    private static String sqlInitialize;
    /** Connection to the H2 test database. */
    private HikariDataSource hikariSource;

    /**
     * Set up the settings mock to return specific values for database settings and load {@link #sqlInitialize}.
     */
    @BeforeAll
    static void initializeSettings() throws IOException, ClassNotFoundException {
        // Check that we have an H2 driver
        Class.forName("org.h2.jdbcx.JdbcDataSource");

        settings = mock(Settings.class);
        TestHelper.returnDefaultsForAllProperties(settings);
        set(DatabaseSettings.MYSQL_DATABASE, "h2_test");
        set(DatabaseSettings.MYSQL_TABLE, "authme");
        TestHelper.setRealLogger();

        Path sqlInitFile = TestHelper.getJarPath(TestHelper.PROJECT_ROOT + "datasource/sql-initialize.sql");
        sqlInitialize = new String(Files.readAllBytes(sqlInitFile));
    }

    @BeforeEach
    void initializeConnectionAndTable() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
        config.setConnectionTestQuery("VALUES 1");
        config.addDataSourceProperty("URL", "jdbc:h2:mem:test;ignorecase=true");
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

    @AfterEach
    void closeConnection() {
        silentClose(hikariSource);
    }

    @Override
    protected DataSource getDataSource(String saltColumn) {
        when(settings.getProperty(DatabaseSettings.MYSQL_COL_SALT)).thenReturn(saltColumn);
        return SqlDataSourceTestUtil.createPostgres(settings, hikariSource);
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
