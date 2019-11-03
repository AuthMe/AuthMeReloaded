package fr.xephi.authme.datasource;

import fr.xephi.authme.TestHelper;
import org.junit.Before;
import org.junit.Test;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
    public void shouldFetchNullableStatus() throws SQLException {
        // given
        String tableName = "data";
        String columnName = "category";
        ResultSet resultSet = mock(ResultSet.class);
        given(resultSet.getInt("NULLABLE")).willReturn(DatabaseMetaData.columnNullable);
        given(resultSet.next()).willReturn(true);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        given(metaData.getColumns(null, null, tableName, columnName)).willReturn(resultSet);

        // when
        boolean result = SqlDataSourceUtils.isNotNullColumn(metaData, tableName, columnName);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldReturnFalseForUnknownNullableStatus() throws SQLException {
        // given
        String tableName = "comments";
        String columnName = "author";
        ResultSet resultSet = mock(ResultSet.class);
        given(resultSet.getInt("NULLABLE")).willReturn(DatabaseMetaData.columnNullableUnknown);
        given(resultSet.next()).willReturn(true);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        given(metaData.getColumns(null, null, tableName, columnName)).willReturn(resultSet);

        // when
        boolean result = SqlDataSourceUtils.isNotNullColumn(metaData, tableName, columnName);

        // then
        assertThat(result, equalTo(false));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowForUnknownColumnInNullableCheck() throws SQLException {
        // given
        String tableName = "data";
        String columnName = "unknown";
        ResultSet resultSet = mock(ResultSet.class);
        given(resultSet.next()).willReturn(false);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        given(metaData.getColumns(null, null, tableName, columnName)).willReturn(resultSet);

        // when
        SqlDataSourceUtils.isNotNullColumn(metaData, tableName, columnName);

        // then - expect exception
    }

    @Test
    public void shouldGetDefaultValue() throws SQLException {
        // given
        String tableName = "data";
        String columnName = "category";
        ResultSet resultSet = mock(ResultSet.class);
        given(resultSet.getObject("COLUMN_DEF")).willReturn("Literature");
        given(resultSet.next()).willReturn(true);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        given(metaData.getColumns(null, null, tableName, columnName)).willReturn(resultSet);

        // when
        Object defaultValue = SqlDataSourceUtils.getColumnDefaultValue(metaData, tableName, columnName);

        // then
        assertThat(defaultValue, equalTo("Literature"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowForUnknownColumnInDefaultValueRetrieval() throws SQLException {
        // given
        String tableName = "data";
        String columnName = "unknown";
        ResultSet resultSet = mock(ResultSet.class);
        given(resultSet.next()).willReturn(false);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        given(metaData.getColumns(null, null, tableName, columnName)).willReturn(resultSet);

        // when
        SqlDataSourceUtils.getColumnDefaultValue(metaData, tableName, columnName);

        // then - expect exception
    }

    @Test
    public void shouldHandleNullDefaultValue() throws SQLException {
        // given
        String tableName = "data";
        String columnName = "category";
        ResultSet resultSet = mock(ResultSet.class);
        given(resultSet.getObject("COLUMN_DEF")).willReturn(null);
        given(resultSet.next()).willReturn(true);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        given(metaData.getColumns(null, null, tableName, columnName)).willReturn(resultSet);

        // when
        Object defaultValue = SqlDataSourceUtils.getColumnDefaultValue(metaData, tableName, columnName);

        // then
        assertThat(defaultValue, nullValue());
    }
}
