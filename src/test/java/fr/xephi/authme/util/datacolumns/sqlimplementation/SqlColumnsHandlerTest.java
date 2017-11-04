package fr.xephi.authme.util.datacolumns.sqlimplementation;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.DataSourceResult;
import fr.xephi.authme.util.datacolumns.Column;
import fr.xephi.authme.util.datacolumns.ColumnType;
import fr.xephi.authme.util.datacolumns.DataSourceValues;
import fr.xephi.authme.util.datacolumns.StandardTypes;
import fr.xephi.authme.util.datacolumns.UpdateValues;
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
import static fr.xephi.authme.util.datacolumns.predicate.StandardPredicates.eq;
import static fr.xephi.authme.util.datacolumns.predicate.StandardPredicates.greaterThan;
import static fr.xephi.authme.util.datacolumns.predicate.StandardPredicates.greaterThanEquals;
import static fr.xephi.authme.util.datacolumns.predicate.StandardPredicates.isNull;
import static fr.xephi.authme.util.datacolumns.predicate.StandardPredicates.notEq;
import static fr.xephi.authme.util.datacolumns.predicate.StandardPredicates.or;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Integration test for {@link SqlColumnsHandler}, using an in-memory SQLite database.
 */
public class SqlColumnsHandlerTest {

    private static final String TABLE_NAME = "testingdata";
    private static final String ID_COLUMN = "id";

    // Columns that are not empty for testing
    private static final ColumnImpl<String> COL_EMAIL = new ColumnImpl<>("email", StandardTypes.STRING);
    private static final ColumnImpl<Long> COL_LAST_LOGIN = new ColumnImpl<>("last_login", StandardTypes.LONG);
    private static final ColumnImpl<Integer> COL_IS_LOCKED = new ColumnImpl<>("is_locked", StandardTypes.INTEGER);

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
        assertThat(alexIp.playerExists(), equalTo(true));
        assertThat(alexIp.getValue(), equalTo("111.111.111.111"));
        assertThat(emilyIp.playerExists(), equalTo(true));
        assertThat(emilyIp.getValue(), nullValue());
        assertThat(nonExistentIp.playerExists(), equalTo(false));
        assertThat(nonExistentIp.getValue(), nullValue());
    }

    @Test
    public void shouldHandleEmptyColumnRetrieval() throws SQLException {
        // given
        context.setEmptyOptions(true, false, false);

        // when
        DataSourceResult<String> result1 = handler.retrieve(2, SampleColumns.EMAIL);
        DataSourceResult<String> result2 = handler.retrieve(777, SampleColumns.EMAIL);

        // then
        assertThat(result1.playerExists(), equalTo(true));
        assertThat(result1.getValue(), nullValue());
        assertThat(result2.playerExists(), equalTo(false));
        assertThat(result2.getValue(), nullValue());
    }

    @Test
    public void shouldRetrieveMultipleValues() throws SQLException {
        // given
        SampleColumns<?>[] columns = { SampleColumns.NAME, SampleColumns.IS_LOCKED, SampleColumns.LAST_LOGIN };

        // when
        DataSourceValues hansValues = handler.retrieve(8, columns);
        DataSourceValues finnValues = handler.retrieve(6, columns);
        DataSourceValues nonExistent = handler.retrieve(-5, columns);

        // then
        assertThat(hansValues.get(SampleColumns.NAME), equalTo("Hans"));
        assertThat(hansValues.get(SampleColumns.IS_LOCKED), equalTo(1));
        assertThat(hansValues.get(SampleColumns.LAST_LOGIN), equalTo(77665544L));
        verifyThrowsException(() -> hansValues.get(SampleColumns.ID));

        assertThat(finnValues.get(SampleColumns.NAME), equalTo("Finn"));
        assertThat(finnValues.get(SampleColumns.IS_LOCKED), equalTo(0));
        assertThat(finnValues.get(SampleColumns.LAST_LOGIN), nullValue());
        verifyThrowsException(() -> finnValues.get(SampleColumns.IS_ACTIVE));

        assertThat(nonExistent.playerExists(), equalTo(false));
        verifyThrowsException(() -> nonExistent.get(SampleColumns.NAME));
    }

    @Test
    public void shouldHandleRetrievalOfMultipleValuesIncludingEmpty() throws SQLException {
        // given
        context.setEmptyOptions(true, false, true);
        SampleColumns<?>[] columns = { SampleColumns.NAME, SampleColumns.IS_LOCKED, SampleColumns.LAST_LOGIN };

        // when
        DataSourceValues hansValues = handler.retrieve(8, columns);
        DataSourceValues finnValues = handler.retrieve(6, columns);
        DataSourceValues nonExistent = handler.retrieve(-5, columns);

        // then
        assertThat(hansValues.get(SampleColumns.NAME), equalTo("Hans"));
        assertThat(hansValues.get(SampleColumns.IS_LOCKED), equalTo(1));
        assertThat(hansValues.get(SampleColumns.LAST_LOGIN), nullValue());
        verifyThrowsException(() -> hansValues.get(SampleColumns.ID));

        assertThat(finnValues.get(SampleColumns.NAME), equalTo("Finn"));
        assertThat(finnValues.get(SampleColumns.IS_LOCKED), equalTo(0));
        assertThat(finnValues.get(SampleColumns.LAST_LOGIN), nullValue());
        verifyThrowsException(() -> finnValues.get(SampleColumns.IS_ACTIVE));

        assertThat(nonExistent.playerExists(), equalTo(false));
        verifyThrowsException(() -> nonExistent.get(SampleColumns.LAST_LOGIN));
    }

    @Test
    public void shouldRetrieveMultipleAllEmptyColumnsSuccessfully() throws SQLException {
        // given
        context.setEmptyOptions(true, false, true);
        SampleColumns<?>[] columns = { SampleColumns.EMAIL, SampleColumns.LAST_LOGIN };

        // when
        DataSourceValues hansValues = handler.retrieve(8, columns);
        DataSourceValues nonExistent = handler.retrieve(-5, columns);

        // then
        assertThat(hansValues.playerExists(), equalTo(true));
        assertThat(hansValues.get(SampleColumns.EMAIL), nullValue());
        assertThat(hansValues.get(SampleColumns.LAST_LOGIN), nullValue());
        verifyThrowsException(() -> hansValues.get(SampleColumns.ID));

        assertThat(nonExistent.playerExists(), equalTo(false));
        verifyThrowsException(() -> nonExistent.get(SampleColumns.LAST_LOGIN));
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
    public void shouldHandleSingleValueUpdateWithEmptyColun() throws SQLException {
        // given
        context.setEmptyOptions(true, false, false);

        // when
        boolean result1 = handler.update(2, SampleColumns.EMAIL, "mailForAlex@example.org");
        boolean result2 = handler.update(999, SampleColumns.EMAIL, "");

        // then
        assertThat(result1, equalTo(true));
        // check that email was not updated
        assertThat(handler.retrieve(2, COL_EMAIL).getValue(), equalTo("test@example.com"));

        assertThat(result2, equalTo(true));
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
    public void shouldPerformMultiUpdateWithEmptyColumns() throws SQLException {
        // given
        context.setEmptyOptions(true, false, true);

        // when
        boolean result1 = handler.update(9,
            with(SampleColumns.IS_LOCKED, 1)
            .and(SampleColumns.EMAIL, null)
            .and(SampleColumns.LAST_LOGIN, 1258L).build());
        boolean result2 = handler.update(-9999,
            with(SampleColumns.IS_LOCKED, 0)
            .and(SampleColumns.EMAIL, "mymail@test.tld")
            .and(SampleColumns.LAST_LOGIN, null).build());

        // then
        assertThat(result1, equalTo(true));
        assertThat(handler.retrieve(9, SampleColumns.IS_LOCKED).getValue(), equalTo(1));
        // assert email / last login unchanged
        assertThat(handler.retrieve(9, COL_EMAIL).getValue(), equalTo("other@test.tld"));
        assertThat(handler.retrieve(9, COL_LAST_LOGIN).getValue(), equalTo(725124L));

        assertThat(result2, equalTo(false));
    }

    @Test
    public void shouldPerformMultiUpdateWithAllEmptyColumns() throws SQLException {
        // given
        context.setEmptyOptions(true, true, true);

        // when
        boolean result1 = handler.update(9,
            with(SampleColumns.IS_LOCKED, 1)
            .and(SampleColumns.EMAIL, null)
            .and(SampleColumns.LAST_LOGIN, 1258L).build());
        boolean result2 = handler.update(-9999,
            with(SampleColumns.IS_LOCKED, 0)
            .and(SampleColumns.EMAIL, "mymail@test.tld")
            .and(SampleColumns.LAST_LOGIN, null).build());

        // then
        assertThat(result1, equalTo(false));
        // assert unchanged
        assertThat(handler.retrieve(9, COL_IS_LOCKED).getValue(), equalTo(0));
        assertThat(handler.retrieve(9, COL_EMAIL).getValue(), equalTo("other@test.tld"));
        assertThat(handler.retrieve(9, COL_LAST_LOGIN).getValue(), equalTo(725124L));

        assertThat(result2, equalTo(false));
    }

    @Test
    public void shouldInsertValues() throws SQLException {
        // given
        UpdateValues<SampleContext> values = UpdateValues
            .with(SampleColumns.ID, 414)
            .and(SampleColumns.NAME, "Oliver")
            .and(SampleColumns.IS_LOCKED, 0)
            .and(SampleColumns.IS_ACTIVE, 1)
            .and(SampleColumns.LAST_LOGIN, 555L)
            .build();

        // when
        boolean result = handler.insert(values);

        // then
        assertThat(result, equalTo(true));
        DataSourceValues retrievedValues = handler.retrieve(414,
            SampleColumns.NAME, SampleColumns.LAST_LOGIN, SampleColumns.IS_ACTIVE);
        assertThat(retrievedValues.get(SampleColumns.NAME), equalTo("Oliver"));
        assertThat(retrievedValues.get(SampleColumns.IS_ACTIVE), equalTo(1));
        assertThat(retrievedValues.get(SampleColumns.LAST_LOGIN), equalTo(555L));
    }

    @Test
    public void shouldHandleInsertWithEmptyColumns() throws SQLException {
        // given
        context.setEmptyOptions(true, false, true);
        UpdateValues<SampleContext> values = UpdateValues
            .with(SampleColumns.ID, 414)
            .and(SampleColumns.NAME, "Oscar")
            .and(SampleColumns.IS_LOCKED, 1)
            .and(SampleColumns.IS_ACTIVE, 1)
            .and(SampleColumns.EMAIL, "value@example.org")
            .and(SampleColumns.LAST_LOGIN, 555L)
            .build();

        // when
        boolean result = handler.insert(values);

        // then
        assertThat(result, equalTo(true));
        DataSourceValues retrievedValues = handler.retrieve(414,
            SampleColumns.NAME, SampleColumns.IS_LOCKED, SampleColumns.IS_ACTIVE, COL_EMAIL, COL_LAST_LOGIN);
        assertThat(retrievedValues.get(SampleColumns.NAME), equalTo("Oscar"));
        assertThat(retrievedValues.get(SampleColumns.IS_LOCKED), equalTo(1));
        assertThat(retrievedValues.get(SampleColumns.IS_ACTIVE), equalTo(1));
        assertThat(retrievedValues.get(COL_EMAIL), nullValue());
        assertThat(retrievedValues.get(COL_LAST_LOGIN), nullValue());
    }

    @Test
    public void shouldThrowExceptionForInsertWithNoNonEmptyColumns() throws SQLException {
        // given
        context.setEmptyOptions(true, true, false);
        UpdateValues<SampleContext> values = UpdateValues
            .with(SampleColumns.EMAIL, "test@example.com")
            .and(SampleColumns.IS_LOCKED, 0)
            .build();

        // when
        try {
            handler.insert(values);

            // then
            fail("Expected exception to be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), startsWith("Cannot perform insert when all columns are empty"));
        }
    }

    @Test
    public void shouldCountWithPredicates() throws SQLException {
        // given / when
        int emailCount = handler.count(eq(SampleColumns.EMAIL, "other@test.tld"));
        int ipLastLoginCount = handler.count(isNull(SampleColumns.IP).and(
            greaterThan(SampleColumns.LAST_LOGIN, 800000L)));
        int hasEmailAndIsActiveCount = handler.count(notEq(SampleColumns.EMAIL, "test@example.com")
            .and(greaterThanEquals(SampleColumns.IS_ACTIVE, 1)));
        int lockedAndActiveSameValue = handler.count(or(
            eq(SampleColumns.IS_ACTIVE, 0).and(eq(SampleColumns.IS_LOCKED, 0)),
            eq(SampleColumns.IS_ACTIVE, 1).and(eq(SampleColumns.IS_LOCKED, 1))));

        // then
        assertThat(emailCount, equalTo(3));
        assertThat(ipLastLoginCount, equalTo(2));
        assertThat(hasEmailAndIsActiveCount, equalTo(2));
        assertThat(lockedAndActiveSameValue, equalTo(4));
    }

    private static void verifyThrowsException(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected exception to be thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("No value available for column"));
        }
    }

    private static final class ColumnImpl<T> implements Column<T, SampleContext> {
        private final String name;
        private final ColumnType<T> type;

        ColumnImpl(String name, ColumnType<T> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String resolveName(SampleContext context) {
            return name;
        }

        @Override
        public ColumnType<T> getType() {
            return type;
        }

        @Override
        public boolean isColumnUsed(SampleContext context) {
            return true;
        }
    }
}
