package fr.xephi.authme.command.executable.authme.debug;

import ch.jalu.injector.factory.SingletonStore;
import com.google.common.cache.LoadingCache;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link DataStatistics}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DataStatisticsTest {

    @InjectMocks
    private DataStatistics dataStatistics;

    @Mock
    private DataSource dataSource;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private LimboService limboService;
    @Mock
    private SingletonStore<Object> singletonStore;

    @Before
    public void setUpLimboCacheMap() {
        Map<String, LimboPlayer> limboMap = new HashMap<>();
        limboMap.put("test", mock(LimboPlayer.class));
        ReflectionTestUtils.setField(LimboService.class, limboService, "entries", limboMap);
    }

    @Test
    public void shouldOutputStatistics() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(singletonStore.retrieveAllOfType()).willReturn(mockListOfSize(Object.class, 7));
        given(singletonStore.retrieveAllOfType(Reloadable.class)).willReturn(mockListOfSize(Reloadable.class, 4));
        given(singletonStore.retrieveAllOfType(SettingsDependent.class)).willReturn(mockListOfSize(SettingsDependent.class, 3));
        given(singletonStore.retrieveAllOfType(HasCleanup.class)).willReturn(mockListOfSize(HasCleanup.class, 2));
        given(dataSource.getAccountsRegistered()).willReturn(219);
        given(playerCache.getLogged()).willReturn(12);

        // Clear any loggers that might exist and trigger the generation of two loggers
        Map loggers = ReflectionTestUtils.getFieldValue(ConsoleLoggerFactory.class, null, "consoleLoggers");
        loggers.clear();
        ConsoleLoggerFactory.get(String.class);
        ConsoleLoggerFactory.get(Integer.class);

        // when
        dataStatistics.execute(sender, Collections.emptyList());

        // then
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(stringCaptor.capture());
        assertThat(stringCaptor.getAllValues(), containsInAnyOrder(
            ChatColor.BLUE + "AuthMe statistics",
            "Singleton Java classes: 7",
            "(Reloadable: 4 / SettingsDependent: 3 / HasCleanup: 2)",
            "LimboPlayers in memory: 1",
            "Total players in DB: 219",
            "PlayerCache size: 12 (= logged in players)",
            "Total logger instances: 2"));
    }

    @Test
    public void shouldOutputCachedDataSourceStatistics() {
        // given
        CacheDataSource cacheDataSource = mock(CacheDataSource.class);
        LoadingCache<String, Optional<PlayerAuth>> cache = mock(LoadingCache.class);
        given(cache.size()).willReturn(11L);
        given(cacheDataSource.getCachedAuths()).willReturn(cache);
        ReflectionTestUtils.setField(DataStatistics.class, dataStatistics, "dataSource", cacheDataSource);
        CommandSender sender = mock(CommandSender.class);

        // when
        dataStatistics.execute(sender, Collections.emptyList());

        // then
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(stringCaptor.capture());
        assertThat(stringCaptor.getAllValues(), hasItem("Cached PlayerAuth objects: 11"));
    }

    private static <T> List<T> mockListOfSize(Class<T> mockClass, int size) {
        T mock = mock(mockClass);
        return Collections.nCopies(size, mock);
    }
}
