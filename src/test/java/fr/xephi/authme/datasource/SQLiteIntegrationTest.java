package fr.xephi.authme.datasource;

import fr.xephi.authme.ConsoleLoggerTestInitializer;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static fr.xephi.authme.datasource.AuthMeMatchers.equalToHash;
import static fr.xephi.authme.datasource.AuthMeMatchers.hasAuthBasicData;
import static fr.xephi.authme.datasource.AuthMeMatchers.hasAuthLocation;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link SQLite}.
 */
public class SQLiteIntegrationTest {

    /** Mock for a settings instance. */
    private static NewSetting settings;
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

        settings = mock(NewSetting.class);
        when(settings.getProperty(any(Property.class))).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return ((Property) invocation.getArguments()[0]).getDefaultValue();
            }
        });
        set(DatabaseSettings.MYSQL_DATABASE, "sqlite-test");
        set(DatabaseSettings.MYSQL_TABLE, "authme");
        set(DatabaseSettings.MYSQL_COL_SALT, "salt");
        ConsoleLoggerTestInitializer.setupLogger();

        Path sqlInitFile = TestHelper.getJarPath("/datasource-integration/sqlite-initialize.sql");
        // Note ljacqu 20160221: It appears that we can only run one statement per Statement.execute() so we split
        // the SQL file by ";\n" as to get the individual statements
        sqlInitialize = new String(Files.readAllBytes(sqlInitFile)).split(";\\n");
    }

    @Before
    public void initializeConnectionAndTable() throws SQLException, ClassNotFoundException {
        silentClose(con);
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS authme");
            for (String statement : sqlInitialize) {
                st.execute(statement);
            }
        }
        con = connection;
    }

    @Test
    public void shouldReturnIfAuthIsAvailableOrNot() {
        // given
        DataSource dataSource = new SQLite(settings, con, false);

        // when
        boolean bobby = dataSource.isAuthAvailable("bobby");
        boolean chris = dataSource.isAuthAvailable("chris");
        boolean user = dataSource.isAuthAvailable("USER");

        // then
        assertThat(bobby, equalTo(true));
        assertThat(chris, equalTo(false));
        assertThat(user, equalTo(true));
    }

    @Test
    public void shouldReturnPassword() {
        // given
        DataSource dataSource = new SQLite(settings, con, false);

        // when
        HashedPassword bobbyPassword = dataSource.getPassword("bobby");
        HashedPassword invalidPassword = dataSource.getPassword("doesNotExist");
        HashedPassword userPassword = dataSource.getPassword("user");

        // then
        assertThat(bobbyPassword, equalToHash(
            "$SHA$11aa0706173d7272$dbba96681c2ae4e0bfdf226d70fbbc5e4ee3d8071faa613bc533fe8a64817d10"));
        assertThat(invalidPassword, nullValue());
        assertThat(userPassword, equalToHash("b28c32f624a4eb161d6adc9acb5bfc5b", "f750ba32"));
    }

    @Test
    public void shouldGetAuth() {
        // given
        DataSource dataSource = new SQLite(settings, con, false);

        // when
        PlayerAuth invalidAuth = dataSource.getAuth("notInDB");
        PlayerAuth bobbyAuth = dataSource.getAuth("Bobby");
        PlayerAuth userAuth = dataSource.getAuth("user");

        // then
        assertThat(invalidAuth, nullValue());

        assertThat(bobbyAuth, hasAuthBasicData("bobby", "Bobby", "your@email.com", "123.45.67.89"));
        assertThat(bobbyAuth, hasAuthLocation(1.05, 2.1, 4.2, "world"));
        assertThat(bobbyAuth.getLastLogin(), equalTo(1449136800L));
        assertThat(bobbyAuth.getPassword(), equalToHash(
            "$SHA$11aa0706173d7272$dbba96681c2ae4e0bfdf226d70fbbc5e4ee3d8071faa613bc533fe8a64817d10"));

        assertThat(userAuth, hasAuthBasicData("user", "user", "user@example.org", "34.56.78.90"));
        assertThat(userAuth, hasAuthLocation(124.1, 76.3, -127.8, "nether"));
        assertThat(userAuth.getLastLogin(), equalTo(1453242857L));
        assertThat(userAuth.getPassword(), equalToHash("b28c32f624a4eb161d6adc9acb5bfc5b", "f750ba32"));
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
