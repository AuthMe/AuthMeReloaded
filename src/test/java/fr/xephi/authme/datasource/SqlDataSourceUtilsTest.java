package fr.xephi.authme.datasource;

import fr.xephi.authme.TestHelper;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

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
}
