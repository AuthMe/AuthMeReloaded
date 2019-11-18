package fr.xephi.authme.datasource;

import ch.jalu.configme.properties.Property;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link SQLite}.
 */
public class SQLiteIntegrationTest extends AbstractDataSourceIntegrationTest {

    /** Mock of a settings instance. */
    private static Settings settings;
    /** Collection of SQL statements to execute for initialization of a test. */
    private static String[] sqlInitialize;
    /** Connection to the SQLite test database. */
    private Connection con;

    /**
     * Set up the settings mock to return specific values for database settings and load {@link #sqlInitialize}.
     */
    @BeforeClass
    public static void initializeSettings() throws IOException, ClassNotFoundException {
        // Check that we have an implementation for SQLite
        Class.forName("org.sqlite.JDBC");

        settings = mock(Settings.class);
        TestHelper.returnDefaultsForAllProperties(settings);
        set(DatabaseSettings.MYSQL_DATABASE, "sqlite-test");
        set(DatabaseSettings.MYSQL_TABLE, "authme");
        TestHelper.setupLogger();

        Path sqlInitFile = TestHelper.getJarPath(TestHelper.PROJECT_ROOT + "datasource/sql-initialize.sql");
        // Note ljacqu 20160221: It appears that we can only run one statement per Statement.execute() so we split
        // the SQL file by ";\n" as to get the individual statements
        sqlInitialize = new String(Files.readAllBytes(sqlInitFile)).split(";(\\r?)\\n");
    }

    @Before
    public void initializeConnectionAndTable() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS authme");
            for (String statement : sqlInitialize) {
                st.execute(statement);
            }
        }
        con = connection;
    }

    @After
    public void closeConnection() {
        silentClose(con);
    }

    @Test
    public void shouldSetUpTableIfMissing() throws SQLException {
        // given
        Statement st = con.createStatement();
        // table is absent
        st.execute("DROP TABLE authme");
        SQLite sqLite = new SQLite(settings, null, con);

        // when
        sqLite.setup();

        // then
        // Save some player to verify database is operational
        sqLite.saveAuth(PlayerAuth.builder().name("Name").build());
        assertThat(sqLite.getAllAuths(), hasSize(1));
    }

    @Test
    public void shouldCreateMissingColumns() throws SQLException {
        // given
        Statement st = con.createStatement();
        // drop table and create one with only some of the columns: SQLite doesn't support ALTER TABLE t DROP COLUMN c
        st.execute("DROP TABLE authme");
        st.execute("CREATE TABLE authme ("
            + "id bigint, "
            + "username varchar(255) unique, "
            + "password varchar(255) not null, "
            + "primary key (id));");
        SQLite sqLite = new SQLite(settings, null, con);

        // when
        sqLite.setup();

        // then
        // Save some player to verify database is operational
        sqLite.saveAuth(PlayerAuth.builder().name("Name").build());
        assertThat(sqLite.getAllAuths(), hasSize(1));
    }

    @Override
    protected DataSource getDataSource(String saltColumn) {
        when(settings.getProperty(DatabaseSettings.MYSQL_COL_SALT)).thenReturn(saltColumn);
        return new SQLite(settings, null, con);
    }

    private static <T> void set(Property<T> property, T value) {
        when(settings.getProperty(property)).thenReturn(value);
    }

    private static void silentClose(Connection con) {
        if (con != null) {
            try {
                if (!con.isClosed()) {
                    con.close();
                }
            } catch (SQLException e) {
                // silent
            }
        }
    }
}
