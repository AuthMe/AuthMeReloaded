package fr.xephi.authme.datasource;

import ch.jalu.configme.properties.Property;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.util.datacolumns.AuthMeColumns;
import fr.xephi.authme.util.datacolumns.DataSourceValues;
import fr.xephi.authme.util.datacolumns.UpdateValues;
import fr.xephi.authme.util.datacolumns.sqlimplementation.AuthMeColumnsHandler;
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
import java.util.stream.Stream;

import static fr.xephi.authme.AuthMeMatchers.equalToHash;
import static fr.xephi.authme.AuthMeMatchers.hasAuthBasicData;
import static fr.xephi.authme.AuthMeMatchers.hasRegistrationInfo;
import static fr.xephi.authme.util.datacolumns.predicate.StandardPredicates.and;
import static fr.xephi.authme.util.datacolumns.predicate.StandardPredicates.eq;
import static fr.xephi.authme.util.datacolumns.predicate.StandardPredicates.isNull;
import static fr.xephi.authme.util.datacolumns.predicate.StandardPredicates.or;
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

    @Test
    public void shouldCountWithPredicate() throws SQLException {
        // given
        PlayerAuth auth1 = PlayerAuth.builder().name("user001").registrationIp("1.1.1.1").email("test@email.org").build();
        PlayerAuth auth2 = PlayerAuth.builder().name("user002").registrationIp("1.1.1.1").email("other@other.tld").build();
        PlayerAuth auth3 = PlayerAuth.builder().name("user003").registrationIp("1.1.1.1").email("test@email.org").build();
        PlayerAuth auth4 = PlayerAuth.builder().name("user004").registrationIp("2.2.2.2").email("other@other.tld").build();
        PlayerAuth auth5 = PlayerAuth.builder().name("user005").registrationIp("2.2.2.2").email("test@email.org").build();

        DataSource ds = getDataSource();
        Stream.of(auth1, auth2, auth3, auth4, auth5).forEach(ds::saveAuth);

        AuthMeColumnsHandler handler = createColumnsHandler();

        // when
        int testMailOr1111IpCount = handler.count(
            eq(AuthMeColumns.EMAIL, "test@email.org").or(eq(AuthMeColumns.REGISTRATION_IP, "1.1.1.1")));
        int testMailAnd1111IpCount = handler.count(
            eq(AuthMeColumns.EMAIL, "test@email.org").and(eq(AuthMeColumns.REGISTRATION_IP, "1.1.1.1")));
        int otherMailCount = handler.count(eq(AuthMeColumns.EMAIL, "other@other.tld"));
        int nonExistentCount = handler.count(eq(AuthMeColumns.EMAIL, "doesNotExist"));
        int ipAndEmailCount = handler.count(or(
            and(eq(AuthMeColumns.EMAIL, "test@email.org"), eq(AuthMeColumns.REGISTRATION_IP, "1.1.1.1")),
            and(eq(AuthMeColumns.EMAIL, "other@other.tld"), eq(AuthMeColumns.REGISTRATION_IP, "2.2.2.2")))
        );
        // TODO: Not predicate does not work with SQLite
        int ip2222OrNull = handler.count(eq(AuthMeColumns.REGISTRATION_IP, "2.2.2.2")
            .or(isNull(AuthMeColumns.REGISTRATION_IP)));

        // then
        assertThat(testMailOr1111IpCount, equalTo(4));
        assertThat(testMailAnd1111IpCount, equalTo(2));
        assertThat(otherMailCount, equalTo(2));
        assertThat(nonExistentCount, equalTo(0));
        assertThat(ipAndEmailCount, equalTo(3));
        assertThat(ip2222OrNull, equalTo(3));
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
