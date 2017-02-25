package fr.xephi.authme.data;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.expiring.ExpiringSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link SessionManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SessionManagerTest {

    @Test
    public void shouldHaveSession() {
        // given
        Settings settings = mockSettings(true, 10);
        SessionManager manager = new SessionManager(settings);
        String player = "playah";

        // when
        manager.addSession(player);

        // then
        assertThat(manager.hasSession(player), equalTo(true));
    }

    @Test
    public void shouldNotHaveSession() {
        // given
        Settings settings = mockSettings(true, 10);
        SessionManager manager = new SessionManager(settings);
        String player = "playah";

        // when/then
        assertThat(manager.hasSession(player), equalTo(false));
    }

    @Test
    public void shouldNotAddSessionBecauseDisabled() {
        // given
        Settings settings = mockSettings(false, 10);
        SessionManager manager = new SessionManager(settings);
        String player = "playah";

        // when
        manager.addSession(player);

        // then
        assertThat(manager.hasSession(player), equalTo(false));
    }

    @Test
    public void shouldNotAddSessionBecauseTimeoutIsZero() {
        // given
        Settings settings = mockSettings(true, 0);
        SessionManager manager = new SessionManager(settings);
        String player = "playah";

        // when
        manager.addSession(player);

        // then
        assertThat(manager.hasSession(player), equalTo(false));
    }

    @Test
    public void shouldRemoveSession() {
        // given
        Settings settings = mockSettings(true, 10);
        String player = "user";
        SessionManager manager = new SessionManager(settings);
        manager.addSession(player);

        // when
        manager.removeSession(player);

        // then
        assertThat(manager.hasSession(player), equalTo(false));
    }

    @Test
    public void shouldClearAllSessionsAfterDisable() {
        // given
        Settings settings = mockSettings(true, 10);
        SessionManager manager = new SessionManager(settings);
        manager.addSession("player01");
        manager.addSession("player02");

        // when
        manager.reload(mockSettings(false, 20));

        // then
        assertThat(getSessionsMap(manager).isEmpty(), equalTo(true));
    }

    @Test
    public void shouldPerformCleanup() {
        // given
        Settings settings = mockSettings(true, 1);
        SessionManager manager = new SessionManager(settings);
        ExpiringSet<String> expiringSet = mockExpiringSet();
        setSessionsMap(manager, expiringSet);

        // when
        manager.performCleanup();

        // then
        verify(expiringSet).removeExpiredEntries();
    }

    @Test
    public void shouldNotPerformCleanup() {
        // given
        Settings settings = mockSettings(false, 1);
        SessionManager manager = new SessionManager(settings);
        ExpiringSet<String> expiringSet = mockExpiringSet();
        setSessionsMap(manager, expiringSet);

        // when
        manager.performCleanup();

        // then
        verify(expiringSet, never()).removeExpiredEntries();
    }

    private static ExpiringSet<String> getSessionsMap(SessionManager manager) {
        return ReflectionTestUtils.getFieldValue(SessionManager.class, manager, "sessions");
    }

    private static void setSessionsMap(SessionManager manager, ExpiringSet<String> sessionsMap) {
        ReflectionTestUtils.setField(SessionManager.class, manager, "sessions", sessionsMap);
    }

    @SuppressWarnings("unchecked")
    private static <T> ExpiringSet<T> mockExpiringSet() {
        return mock(ExpiringSet.class);
    }

    private static Settings mockSettings(boolean isEnabled, int sessionTimeout) {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.SESSIONS_ENABLED)).willReturn(isEnabled);
        given(settings.getProperty(PluginSettings.SESSIONS_TIMEOUT)).willReturn(sessionTimeout);
        return settings;
    }
}
