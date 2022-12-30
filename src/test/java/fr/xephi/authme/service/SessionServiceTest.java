package fr.xephi.authme.service;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.RestoreSessionEvent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.function.Function;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link SessionService}.
 */
@RunWith(DelayedInjectionRunner.class)
public class SessionServiceTest {

    @InjectDelayed
    private SessionService sessionService;

    @Mock
    private DataSource dataSource;
    @Mock
    private CommonService commonService;
    @Mock
    private BukkitService bukkitService;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @BeforeInjecting
    public void setUpEnabledProperty() {
        given(commonService.getProperty(PluginSettings.SESSIONS_ENABLED)).willReturn(true);
    }

    @Test
    public void shouldCheckSessionsEnabledSetting() {
        // given
        Player player = mock(Player.class);
        given(commonService.getProperty(PluginSettings.SESSIONS_ENABLED)).willReturn(false);
        sessionService.reload();

        // when
        boolean result = sessionService.canResumeSession(player);

        // then
        assertThat(result, equalTo(false));
        verifyNoInteractions(dataSource);
    }

    @Test
    public void shouldCheckIfUserHasSession() {
        // given
        String name = "Bobby";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        given(dataSource.hasSession(name)).willReturn(false);

        // when
        boolean result = sessionService.canResumeSession(player);

        // then
        assertThat(result, equalTo(false));
        verify(commonService, only()).getProperty(PluginSettings.SESSIONS_ENABLED);
        verify(dataSource, only()).hasSession(name);
    }

    @Test
    public void shouldCheckLastLoginDate() {
        // given
        String name = "Bobby";
        String ip = "127.3.12.15";
        Player player = mockPlayerWithNameAndIp(name, ip);
        given(commonService.getProperty(PluginSettings.SESSIONS_TIMEOUT)).willReturn(8);
        given(dataSource.hasSession(name)).willReturn(true);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .lastLogin(System.currentTimeMillis() - 10 * 60 * 1000)
            .lastIp(ip).build();
        given(dataSource.getAuth(name)).willReturn(auth);

        // when
        boolean result = sessionService.canResumeSession(player);

        // then
        assertThat(result, equalTo(false));
        verify(commonService).getProperty(PluginSettings.SESSIONS_ENABLED);
        verify(dataSource).hasSession(name);
        verify(dataSource).setUnlogged(name);
        verify(dataSource).revokeSession(name);
    }

    @Test
    public void shouldRefuseSessionForAuthWithNullLastLoginTimestamp() {
        // given
        String name = "Bobby";
        String ip = "127.3.12.15";
        Player player = mockPlayerWithNameAndIp(name, ip);
        given(commonService.getProperty(PluginSettings.SESSIONS_TIMEOUT)).willReturn(8);
        given(dataSource.hasSession(name)).willReturn(true);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .lastLogin(null)
            .lastIp(ip).build();
        given(dataSource.getAuth(name)).willReturn(auth);

        // when
        boolean result = sessionService.canResumeSession(player);

        // then
        assertThat(result, equalTo(false));
        verify(commonService).getProperty(PluginSettings.SESSIONS_ENABLED);
        verify(dataSource).hasSession(name);
        verify(dataSource).setUnlogged(name);
        verify(dataSource).revokeSession(name);
    }

    @Test
    public void shouldCheckLastLoginIp() {
        // given
        String name = "Bobby";
        String ip = "127.3.12.15";
        Player player = mockPlayerWithNameAndIp(name, ip);
        given(dataSource.hasSession(name)).willReturn(true);
        given(commonService.getProperty(PluginSettings.SESSIONS_TIMEOUT)).willReturn(8);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .lastLogin(System.currentTimeMillis() - 7 * 60 * 1000)
            .lastIp("8.8.8.8").build();
        given(dataSource.getAuth(name)).willReturn(auth);

        // when
        boolean result = sessionService.canResumeSession(player);

        // then
        assertThat(result, equalTo(false));
        verify(commonService).getProperty(PluginSettings.SESSIONS_ENABLED);
        verify(commonService).send(player, MessageKey.SESSION_EXPIRED);
        verify(dataSource).hasSession(name);
        verify(dataSource).setUnlogged(name);
        verify(dataSource).revokeSession(name);
    }

    @Test
    public void shouldEmitEventForValidSession() {
        // given
        String name = "Bobby";
        String ip = "127.3.12.15";
        Player player = mockPlayerWithNameAndIp(name, ip);
        given(commonService.getProperty(PluginSettings.SESSIONS_TIMEOUT)).willReturn(8);
        given(dataSource.hasSession(name)).willReturn(true);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .lastLogin(System.currentTimeMillis() - 7 * 60 * 1000)
            .lastIp(ip).build();
        given(dataSource.getAuth(name)).willReturn(auth);
        RestoreSessionEvent event = spy(new RestoreSessionEvent(player, false));
        given(bukkitService.createAndCallEvent(any(Function.class))).willReturn(event);

        // when
        boolean result = sessionService.canResumeSession(player);

        // then
        assertThat(result, equalTo(true));
        verify(commonService).getProperty(PluginSettings.SESSIONS_ENABLED);
        verify(commonService).getProperty(PluginSettings.SESSIONS_TIMEOUT);
        verifyNoMoreInteractions(commonService);
        verify(dataSource).hasSession(name);
        verify(dataSource).setUnlogged(name);
        verify(dataSource).revokeSession(name);
        verify(event).isCancelled();
    }

    @Test
    public void shouldHandleNullPlayerAuth() {
        // given
        String name = "Bobby";
        Player player = mockPlayerWithNameAndIp(name, "127.3.12.15");
        given(dataSource.hasSession(name)).willReturn(true);
        given(dataSource.getAuth(name)).willReturn(null);

        // when
        boolean result = sessionService.canResumeSession(player);

        // then
        assertThat(result, equalTo(false));
        verify(commonService).getProperty(PluginSettings.SESSIONS_ENABLED);
        verify(dataSource).hasSession(name);
        verify(dataSource).setUnlogged(name);
        verify(dataSource).revokeSession(name);
        verify(dataSource).getAuth(name);
    }

    @Test
    public void shouldHandlePlayerAuthWithNullLastIp() {
        // given
        String name = "Charles";
        Player player = mockPlayerWithNameAndIp(name, "144.117.118.145");
        given(dataSource.hasSession(name)).willReturn(true);
        given(commonService.getProperty(PluginSettings.SESSIONS_TIMEOUT)).willReturn(8);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .lastIp(null)
            .lastLogin(System.currentTimeMillis()).build();
        given(dataSource.getAuth(name)).willReturn(auth);

        // when
        boolean result = sessionService.canResumeSession(player);

        // then
        assertThat(result, equalTo(false));
        verify(commonService).getProperty(PluginSettings.SESSIONS_ENABLED);
        verify(dataSource).hasSession(name);
        verify(dataSource).setUnlogged(name);
        verify(dataSource).revokeSession(name);
        verify(dataSource).getAuth(name);
    }

    private static Player mockPlayerWithNameAndIp(String name, String ip) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        TestHelper.mockIpAddressToPlayer(player, ip);
        return player;
    }
}
