package fr.xephi.authme.datasource;

import fr.xephi.authme.TestHelper;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link SqlDataSourceUtils}.
 */
public class SqlDataSourceUtilsTest {

    private Logger logger;

    @Before
    public void initLogger() {
        logger = TestHelper.setupLogger();
    }

    @Test
    public void shouldHaveHiddenConstructor() {
        TestHelper.validateHasOnlyPrivateEmptyConstructor(SqlDataSourceUtils.class);
    }

    @Test
    public void shouldLogException() {
        // given
        String msg = "Hocus pocus did not work";
        SQLException ex = new SQLException(msg);

        // when
        SqlDataSourceUtils.logSqlException(ex);

        // then
        verify(logger).warning(argThat(containsString(msg)));
    }

    @Test
    public void shouldCloseStatement() throws SQLException {
        // given
        Statement st = mock(Statement.class);

        // when
        SqlDataSourceUtils.close(st);

        // then
        verify(st).close();
    }

    @Test
    public void shouldHandleExceptionFromStatement() throws SQLException {
        // given
        Statement st = mock(Statement.class);
        doThrow(SQLException.class).when(st).close();

        // when
        SqlDataSourceUtils.close(st);

        // then
        verify(logger).warning(anyString());
    }

    @Test
    public void shouldCloseResultSet() throws SQLException {
        // given
        ResultSet rs = mock(ResultSet.class);

        // when
        SqlDataSourceUtils.close(rs);

        // then
        verify(rs).close();
    }

    @Test
    public void shouldHandleExceptionFromResultSet() throws SQLException {
        // given
        ResultSet rs = mock(ResultSet.class);
        doThrow(SQLException.class).when(rs).close();

        // when
        SqlDataSourceUtils.close(rs);

        // then
        verify(logger).warning(anyString());
    }

    @Test
    public void shouldCloseConnection() throws SQLException {
        // given
        Connection con = mock(Connection.class);

        // when
        SqlDataSourceUtils.close(con);

        // then
        verify(con).close();
    }

    @Test
    public void shouldHandleExceptionFromConnection() throws SQLException {
        // given
        Connection con = mock(Connection.class);
        doThrow(SQLException.class).when(con).close();

        // when
        SqlDataSourceUtils.close(con);

        // then
        verify(logger).warning(anyString());
    }

    @Test
    public void shouldHandleNullArgument() {
        // given / when
        SqlDataSourceUtils.close((Statement) null);
        SqlDataSourceUtils.close((ResultSet) null);
        SqlDataSourceUtils.close((Connection) null);

        // then - nothing happens
    }

}
