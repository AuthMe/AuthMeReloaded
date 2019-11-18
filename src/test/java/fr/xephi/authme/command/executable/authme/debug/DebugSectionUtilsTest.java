package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.Location;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link DebugSectionUtils}.
 */
public class DebugSectionUtilsTest {

    @Before
    public void initMockLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldFormatLocation() {
        // given / when
        String result = DebugSectionUtils.formatLocation(0.0, 10.248592, -18934.2349023, "Main");

        // then
        assertThat(result, equalTo("(0, 10.25, -18934.23) in 'Main'"));
    }

    @Test
    public void shouldHandleNullWorld() {
        // given
        Location location = new Location(null, 3.7777, 2.14156, 1);

        // when
        String result = DebugSectionUtils.formatLocation(location);

        // then
        assertThat(result, equalTo("(3.78, 2.14, 1) in 'null'"));
    }

    @Test
    public void shouldHandleNullLocation() {
        // given / when / then
        assertThat(DebugSectionUtils.formatLocation(null), equalTo("null"));
    }

    @Test
    public void shouldFetchMapInLimboService() {
        // given
        LimboService limboService = mock(LimboService.class);
        Map<String, LimboPlayer> limboMap = new HashMap<>();
        ReflectionTestUtils.setField(LimboService.class, limboService, "entries", limboMap);

        // when
        Map map = DebugSectionUtils.applyToLimboPlayersMap(limboService, Function.identity());

        // then
        assertThat(map, sameInstance(limboMap));
    }

    @Test
    public void shouldHandleErrorGracefully() {
        // given
        LimboService limboService = mock(LimboService.class);
        Map<String, LimboPlayer> limboMap = new HashMap<>();
        ReflectionTestUtils.setField(LimboService.class, limboService, "entries", limboMap);

        // when
        Object result = DebugSectionUtils.applyToLimboPlayersMap(limboService, map -> {
           throw new IllegalStateException();
        });

        // then
        assertThat(result, nullValue());
    }

    @Test
    public void shouldReturnSameDataSourceInstance() {
        // given
        DataSource dataSource = mock(DataSource.class);

        // when
        DataSource result = DebugSectionUtils.unwrapSourceFromCacheDataSource(dataSource);

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
        DataSource result = DebugSectionUtils.unwrapSourceFromCacheDataSource(cacheDataSource);

        // then
        assertThat(result, equalTo(source));
    }

    @Test
    public void shouldCastOrReturnNull() {
        // given / when / then
        assertThat(DebugSectionUtils.castToTypeOrNull("test", String.class), equalTo("test"));
        assertThat(DebugSectionUtils.castToTypeOrNull("test", Integer.class), nullValue());
        assertThat(DebugSectionUtils.castToTypeOrNull(5, String.class), nullValue());
        assertThat(DebugSectionUtils.castToTypeOrNull(5, Integer.class), equalTo(5));
    }
}
