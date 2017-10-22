package fr.xephi.authme.datasource;

import ch.jalu.configme.properties.Property;
import fr.xephi.authme.AuthMeMatchers;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.sqlcolumns.Column;
import fr.xephi.authme.datasource.sqlcolumns.DataSourceValues;
import fr.xephi.authme.datasource.sqlcolumns.SqliteTestExt;
import fr.xephi.authme.datasource.sqlcolumns.UpdateValues;
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

import static org.hamcrest.Matchers.equalTo;
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
        SQLite sqLite = new SQLite(settings, con);

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
        SQLite sqLite = new SQLite(settings, con);

        // when
        sqLite.setup();

        // then
        // Save some player to verify database is operational
        sqLite.saveAuth(PlayerAuth.builder().name("Name").build());
        assertThat(sqLite.getAllAuths(), hasSize(1));
    }

    @Test
    public void shouldUpdateValues() {
        // given
        UpdateValues values = UpdateValues.builder()
            .put(Column.REALNAME, "BoBBy")
            .put(Column.EMAIL, "bobbers@example.com")
            .put(Column.REGISTRATION_DATE, 123456L)
            .build();
        SqliteTestExt ds = (SqliteTestExt) getDataSource();

        // when
        ds.update("bobby", values);

        // then
        PlayerAuth auth = getDataSource().getAuth("bobby");
        assertThat(auth, AuthMeMatchers.hasAuthBasicData("bobby", "BoBBy", "bobbers@example.com", "123.45.67.89"));
        assertThat(auth.getRegistrationDate(), equalTo(123456L));
    }

    @Test
    public void shouldGetValues() {
        // given
        SqliteTestExt ds = (SqliteTestExt) getDataSource();

        // when
        DataSourceValues result = ds.retrieve("bobby",
            Column.LAST_IP, Column.EMAIL, Column.REALNAME, Column.REGISTRATION_DATE);

        // then
        assertThat(result.get(Column.LAST_IP), equalTo("123.45.67.89"));
        assertThat(result.get(Column.EMAIL), equalTo("your@email.com")); // TODO: should be null?
        assertThat(result.get(Column.REALNAME), equalTo("Bobby"));
        assertThat(result.get(Column.REGISTRATION_DATE), equalTo(1436778723L));
    }

    @Test
    public void shouldGetSingleValue() {
        // given
        SqliteTestExt ds = (SqliteTestExt) getDataSource();

        // when
        DataSourceResult<String> result = ds.retrieve("bobby", Column.LAST_IP);

        // then
        assertThat(result.getValue(), equalTo("123.45.67.89"));
    }

    @Test
    public void shouldHandleUnknownUser() {
        // given
        SqliteTestExt ds = (SqliteTestExt) getDataSource();

        // when
        DataSourceValues result = ds.retrieve("doesNotExist", Column.LAST_IP, Column.REGISTRATION_DATE);

        // then
        assertThat(result.playerExists(), equalTo(false));
    }

    @Override
    protected DataSource getDataSource(String saltColumn) {
        when(settings.getProperty(DatabaseSettings.MYSQL_COL_SALT)).thenReturn(saltColumn);
        return new SqliteTestExt(settings, con);
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
