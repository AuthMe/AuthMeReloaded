package fr.xephi.authme.util.datacolumns.sqlimplementation;

import fr.xephi.authme.TestHelper;
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Integration test for {@link SqlColumnsHandler}, using an in-memory SQLite database.
 */
public class SqlColumnsHandlerTest {

    private static final String TABLE_NAME = "testingdata";
    private static final String ID_COLUMN = "id";

    private Connection con;

    @Before
    public void setUpConnection() throws Exception {
        Class.forName("org.sqlite.JDBC");

        Path sqlInitFile = TestHelper.getJarPath(TestHelper.PROJECT_ROOT + "util/datacolumns/sample-database.sql");
        // Note ljacqu 20160221: It appears that we can only run one statement per Statement.execute() so we split
        // the SQL file by ";\n" as to get the individual statements
        String[] sqlInitialize = new String(Files.readAllBytes(sqlInitFile)).split(";(\\r?)\\n");

        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = connection.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + TABLE_NAME);
            for (String statement : sqlInitialize) {
                st.execute(statement);
            }
        }
        con = connection;
    }

    @After
    public void tearDownConnection() throws Exception {
        if (con != null) {
            con.close();
        }
    }

    @Test
    public void shouldCountWithPredicates() throws SQLException {
        // given
        SampleContext context = new SampleContext(false, false, false);
        SqlColumnsHandler<SampleContext, Integer> handler =
            new SqlColumnsHandler<>(con, context, TABLE_NAME, ID_COLUMN);

        // when
        int emailCount = handler.count(StandardPredicates.eq(SampleColumns.EMAIL, "other@test.tld"));

        // then
        assertThat(emailCount, equalTo(3));
    }

}
