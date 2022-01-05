package fr.xephi.authme.command.executable.authme.debug;

import com.zaxxer.hikari.HikariDataSource;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.datasource.MySQL;
import fr.xephi.authme.datasource.SqlDataSourceTestUtil;
import fr.xephi.authme.settings.Settings;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link MySqlDefaultChanger}.
 */
@RunWith(MockitoJUnitRunner.class)
public class MySqlDefaultChangerTest {

    @Mock
    private Settings settings;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldReturnMySqlConnection() throws SQLException {
        // given
        Settings settings = mock(Settings.class);
        TestHelper.returnDefaultsForAllProperties(settings);
        HikariDataSource dataSource = mock(HikariDataSource.class);
        Connection connection = mock(Connection.class);
        given(dataSource.getConnection()).willReturn(connection);
        MySQL mySQL = SqlDataSourceTestUtil.createMySql(settings, dataSource);
        MySqlDefaultChanger defaultChanger = createDefaultChanger(mySQL);

        // when
        Connection result = defaultChanger.getConnection(mySQL);

        // then
        assertThat(result, equalTo(connection));
        verify(dataSource).getConnection();
    }

    @Test
    public void shouldSetMySqlFieldOnInitialization() {
        // given
        MySQL mySql = mock(MySQL.class);
        MySqlDefaultChanger defaultChanger = createDefaultChanger(mySql);

        // when
        defaultChanger.setMySqlField();

        // then
        assertThat(ReflectionTestUtils.getFieldValue(MySqlDefaultChanger.class, defaultChanger, "mySql"),
            sameInstance(mySql));
    }

    @Test
    public void shouldLeaveMySqlFieldToNullOnInitialization() {
        // given
        DataSource dataSource = mock(DataSource.class);
        PlayerCache playerCache = mock(PlayerCache.class);
        CacheDataSource cacheDataSource = new CacheDataSource(dataSource, playerCache);
        MySqlDefaultChanger defaultChanger = createDefaultChanger(cacheDataSource);

        // when
        defaultChanger.setMySqlField();

        // then
        assertThat(ReflectionTestUtils.getFieldValue(MySqlDefaultChanger.class, defaultChanger, "mySql"),
            nullValue());
    }

    private MySqlDefaultChanger createDefaultChanger(DataSource dataSource) {
        MySqlDefaultChanger defaultChanger = new MySqlDefaultChanger();
        ReflectionTestUtils.setField(defaultChanger, "dataSource", dataSource);
        ReflectionTestUtils.setField(defaultChanger, "settings", settings);
        return defaultChanger;
    }
}
