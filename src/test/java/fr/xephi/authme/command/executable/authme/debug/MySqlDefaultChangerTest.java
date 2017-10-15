package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.settings.Settings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link MySqlDefaultChanger}.
 */
@RunWith(MockitoJUnitRunner.class)
public class MySqlDefaultChangerTest {

    @Mock
    private Settings settings;

    @Test
    public void shouldReturnSameDataSourceInstance() {
        // given
        DataSource dataSource = mock(DataSource.class);

        // when
        DataSource result = MySqlDefaultChanger.unwrapSourceFromCacheDataSource(dataSource);

        // then
        assertThat(result, equalTo(dataSource));
    }

    @Test
    public void shouldUnwrapCacheDataSource() {
        // given
        DataSource source = mock(DataSource.class);
        PlayerCache playerCache = mock(PlayerCache.class);
        CacheDataSource cacheDataSource = new CacheDataSource(source, playerCache);

        // when
        DataSource result = MySqlDefaultChanger.unwrapSourceFromCacheDataSource(cacheDataSource);

        // then
        assertThat(result, equalTo(source));
    }

    // TODO #792: Add more tests

    private MySqlDefaultChanger createDefaultChanger(DataSource dataSource) {
        MySqlDefaultChanger defaultChanger = new MySqlDefaultChanger();
        ReflectionTestUtils.setField(defaultChanger, "dataSource", dataSource);
        ReflectionTestUtils.setField(defaultChanger, "settings", settings);
        return defaultChanger;
    }
}
