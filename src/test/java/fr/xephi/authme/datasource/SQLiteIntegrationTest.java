package fr.xephi.authme.datasource;

import ch.jalu.configme.properties.Property;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.sqlcolumns.AuthMeColumns;
import fr.xephi.authme.datasource.sqlcolumns.DataSourceValues;
import fr.xephi.authme.datasource.sqlcolumns.UpdateValues;
import fr.xephi.authme.datasource.sqlcolumns.sqlimplementation.AuthMeColumnsHandler;
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

import static fr.xephi.authme.AuthMeMatchers.equalToHash;
import static fr.xephi.authme.AuthMeMatchers.hasAuthBasicData;
import static fr.xephi.authme.AuthMeMatchers.hasRegistrationInfo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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
        TestHelper.setRealLogger();

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

    private AuthMeColumnsHandler createColumnsHandler() {
        return new AuthMeColumnsHandler(con, new Columns(settings), settings.getProperty(DatabaseSettings.MYSQL_TABLE),
            settings.getProperty(DatabaseSettings.MYSQL_COL_NAME));
    }

    @Test
    public void shouldUpdateValues() {
        // given
        UpdateValues<Columns> values = UpdateValues
            .with(AuthMeColumns.REALNAME, "BoBBy")
            .and(AuthMeColumns.EMAIL, "bobbers@example.com")
            .and(AuthMeColumns.REGISTRATION_DATE, 123456L)
            .build();
        AuthMeColumnsHandler ds = createColumnsHandler();

        // when
        boolean wasSuccessful = ds.update("bobby", values);

        // then
        assertThat(wasSuccessful, equalTo(true));
        PlayerAuth auth = getDataSource().getAuth("bobby");
        assertThat(auth, hasAuthBasicData("bobby", "BoBBy", "bobbers@example.com", "123.45.67.89"));
        assertThat(auth.getRegistrationDate(), equalTo(123456L));
    }

    @Test
    public void shouldHandleUpdateOnUnknownUser() {
        // given
        UpdateValues<Columns> values = UpdateValues
            .with(AuthMeColumns.EMAIL, "test@tld.tld")
            .and(AuthMeColumns.LAST_IP, "144.144.144.144")
            .build();
        AuthMeColumnsHandler ds = createColumnsHandler();

        // when
        boolean wasSuccessful = ds.update("bogus", values);

        // then
        assertThat(wasSuccessful, equalTo(false));
        PlayerAuth bogusAuth = getDataSource().getAuth("bogus");
        assertThat(bogusAuth, nullValue());
    }

    @Test
    public void shouldGetValues() {
        // given
        AuthMeColumnsHandler ds = createColumnsHandler();

        // when
        DataSourceValues result = ds.retrieve("bobby",
            AuthMeColumns.LAST_IP, AuthMeColumns.EMAIL, AuthMeColumns.REALNAME, AuthMeColumns.REGISTRATION_DATE);

        // then
        assertThat(result.get(AuthMeColumns.LAST_IP), equalTo("123.45.67.89"));
        assertThat(result.get(AuthMeColumns.EMAIL), equalTo("your@email.com"));
        assertThat(result.get(AuthMeColumns.REALNAME), equalTo("Bobby"));
        assertThat(result.get(AuthMeColumns.REGISTRATION_DATE), equalTo(1436778723L));
    }

    @Test
    public void shouldGetSingleValue() {
        // given
        AuthMeColumnsHandler ds = createColumnsHandler();

        // when
        DataSourceResult<String> result = ds.retrieve("bobby", AuthMeColumns.LAST_IP);

        // then
        assertThat(result.getValue(), equalTo("123.45.67.89"));
    }

    @Test
    public void shouldHandleUnknownUser() {
        // given
        AuthMeColumnsHandler ds = createColumnsHandler();

        // when
        DataSourceValues result = ds.retrieve("doesNotExist", AuthMeColumns.LAST_IP, AuthMeColumns.REGISTRATION_DATE);

        // then
        assertThat(result.playerExists(), equalTo(false));
    }

    @Test
    public void shouldInsertValues() {
        // given
        AuthMeColumnsHandler handler = createColumnsHandler();

        // when
        boolean wasSuccessful = handler.insert(UpdateValues
            .with(AuthMeColumns.EMAIL, "my.mail@example.org")
            .and(AuthMeColumns.NAME, "kmfdm")
            .and(AuthMeColumns.PASSWORD_HASH, "test")
            .and(AuthMeColumns.REALNAME, "KMfDM")
            .and(AuthMeColumns.REGISTRATION_DATE, 1444L)
            .and(AuthMeColumns.LAST_IP, "144.117.11.12").build());

        // then
        assertThat(wasSuccessful, equalTo(true));
        DataSource ds = getDataSource();
        PlayerAuth auth = ds.getAuth("kmfdm");
        assertThat(auth, not(nullValue()));
        assertThat(auth, hasAuthBasicData("kmfdm", "KMfDM", "my.mail@example.org", "144.117.11.12"));
        assertThat(auth.getPassword(), equalToHash("test"));
        assertThat(auth.getRegistrationDate(), equalTo(1444L));
    }

    @Test
    public void shouldInsertValuesFromAuth() {
        // given
        AuthMeColumnsHandler handler = createColumnsHandler();
        PlayerAuth auth = PlayerAuth.builder()
            .name("jonathan")
            .registrationDate(1234567L)
            .registrationIp("124.56.78.99")
            .lastLogin(123L) // <-- ignored
            .realName("JONathan") // <-- ignored
            .password("SHA256$abcd123", null).build();

        // when
        boolean wasSuccessful = handler.insert(auth, AuthMeColumns.EMAIL, AuthMeColumns.NAME, AuthMeColumns.REGISTRATION_DATE,
            AuthMeColumns.REGISTRATION_IP, AuthMeColumns.PASSWORD_HASH);

        // then
        assertThat(wasSuccessful, equalTo(true));
        DataSource ds = getDataSource();
        PlayerAuth result = ds.getAuth("jonathan");
        assertThat(result, not(nullValue()));
        assertThat(result, hasAuthBasicData("jonathan", "Player", null, null));
        assertThat(result.getLastLogin(), nullValue());
        assertThat(result.getPassword(), equalToHash("SHA256$abcd123"));
        assertThat(result, hasRegistrationInfo("124.56.78.99", 1234567L));
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
