package fr.xephi.authme.util.datacolumns.sqlimplementation;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.DataSourceResult;
import fr.xephi.authme.util.datacolumns.DataSourceValues;
import fr.xephi.authme.util.datacolumns.predicate.StandardPredicates;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static fr.xephi.authme.util.datacolumns.UpdateValues.with;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Integration test for {@link SqlColumnsHandler}, using an in-memory SQLite database.
 */
public class SqlColumnsHandlerTest {

    private static final String TABLE_NAME = "testingdata";
    private static final String ID_COLUMN = "id";

    private Connection connection;
    private SqlColumnsHandler<SampleContext, Integer> handler;
    private SampleContext context;

    @Before
    public void setUpConnection() throws Exception {
        Class.forName("org.sqlite.JDBC");

        Path sqlInitFile = TestHelper.getJarPath(TestHelper.PROJECT_ROOT + "util/datacolumns/sample-database.sql");
        // Note ljacqu 20160221: It appears that we can only run one statement per Statement.execute() so we split
        // the SQL file by ";\n" as to get the individual statements
        String[] sqlInitialize = new String(Files.readAllBytes(sqlInitFile)).split(";(\\r?)\\n");

        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + TABLE_NAME);
            for (String statement : sqlInitialize) {
                st.execute(statement);
            }
        }

        context = new SampleContext();
        handler = new SqlColumnsHandler<>(connection, context, TABLE_NAME, ID_COLUMN);
    }

    @After
    public void tearDownConnection() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void shouldRetrieveSingleValue() throws SQLException {
        // given / when
        DataSourceResult<String> alexIp = handler.retrieve(1, SampleColumns.IP);
        DataSourceResult<String> emilyIp = handler.retrieve(5, SampleColumns.IP);
        DataSourceResult<String> nonExistentIp = handler.retrieve(222, SampleColumns.IP);

        // then
        assertThat(alexIp.getValue(), equalTo("111.111.111.111"));
        assertThat(emilyIp.playerExists(), equalTo(true));
        assertThat(emilyIp.getValue(), nullValue());
        assertThat(nonExistentIp.playerExists(), equalTo(false));
        assertThat(nonExistentIp.getValue(), nullValue());
    }

    @Test
    public void shouldRetrieveMultipleValues() throws SQLException {
        // given
        SampleColumns<?>[] columns = { SampleColumns.NAME, SampleColumns.IS_LOCKED, SampleColumns.LAST_LOGIN };

        // when
        DataSourceValues brettValues = handler.retrieve(8, columns);
        DataSourceValues finnValues = handler.retrieve(6, columns);
        DataSourceValues nonExistent = handler.retrieve(-5, columns);

        // then
        assertThat(brettValues.get(SampleColumns.NAME), equalTo("Hans"));
        assertThat(brettValues.get(SampleColumns.IS_LOCKED), equalTo(1));
        assertThat(brettValues.get(SampleColumns.LAST_LOGIN), equalTo(77665544L));
        verifyThrowsException(() -> brettValues.get(SampleColumns.ID));

        assertThat(finnValues.get(SampleColumns.NAME), equalTo("Finn"));
        assertThat(finnValues.get(SampleColumns.IS_LOCKED), equalTo(0));
        assertThat(finnValues.get(SampleColumns.LAST_LOGIN), nullValue());
        verifyThrowsException(() -> finnValues.get(SampleColumns.IS_ACTIVE));

        assertThat(nonExistent.playerExists(), equalTo(false));
        verifyThrowsException(() -> nonExistent.get(SampleColumns.NAME));
    }

    @Test
    public void shouldPerformSingleValueUpdate() throws SQLException {
        // given / when
        boolean result1 = handler.update(1, SampleColumns.EMAIL, "mailForAlex@example.org");
        boolean result2 = handler.update(2, SampleColumns.EMAIL, (String) null);
        boolean result3 = handler.update(999, SampleColumns.EMAIL, "");

        // then
        assertThat(result1, equalTo(true));
        assertThat(handler.retrieve(1, SampleColumns.EMAIL).getValue(), equalTo("mailForAlex@example.org"));
        assertThat(result2, equalTo(true));
        assertThat(handler.retrieve(2, SampleColumns.EMAIL).getValue(), nullValue());
        assertThat(result3, equalTo(false));
    }

    @Test
    public void shouldPerformMultiValueUpdate() throws SQLException {
        // given / when
        boolean result1 = handler.update(9,
            with(SampleColumns.IS_LOCKED, 1)
            .and(SampleColumns.EMAIL, null)
            .and(SampleColumns.LAST_LOGIN, 1258L).build());
        boolean result2 = handler.update(12,
            with(SampleColumns.IS_LOCKED, 0)
            .and(SampleColumns.EMAIL, "mymail@test.tld")
            .and(SampleColumns.LAST_LOGIN, null).build());
        boolean result3 = handler.update(-9999,
            with(SampleColumns.IS_LOCKED, 0)
            .and(SampleColumns.EMAIL, "mymail@test.tld")
            .and(SampleColumns.LAST_LOGIN, null).build());

        // then
        assertThat(result1, equalTo(true));
        DataSourceValues igorValues = handler.retrieve(9, SampleColumns.IS_LOCKED, SampleColumns.EMAIL, SampleColumns.LAST_LOGIN);
        assertThat(igorValues.get(SampleColumns.IS_LOCKED), equalTo(1));
        assertThat(igorValues.get(SampleColumns.EMAIL), equalTo(null));
        assertThat(igorValues.get(SampleColumns.LAST_LOGIN), equalTo(1258L));

        assertThat(result2, equalTo(true));
        DataSourceValues louisValues = handler.retrieve(12, SampleColumns.IS_LOCKED, SampleColumns.EMAIL, SampleColumns.LAST_LOGIN);
        assertThat(louisValues.get(SampleColumns.IS_LOCKED), equalTo(0));
        assertThat(louisValues.get(SampleColumns.EMAIL), equalTo("mymail@test.tld"));
        assertThat(louisValues.get(SampleColumns.LAST_LOGIN), equalTo(null));

        assertThat(result3, equalTo(false));
    }

    @Test
    public void shouldCountWithPredicates() throws SQLException {
        // given / when
        int emailCount = handler.count(StandardPredicates.eq(SampleColumns.EMAIL, "other@test.tld"));

        // then
        assertThat(emailCount, equalTo(3));
    }

    private static void verifyThrowsException(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected exception to be thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("No value available for column"));
        }
    }
}
