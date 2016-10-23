package fr.xephi.authme.data;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
    public void shouldDenySessionIfTimeoutHasExpired() {
        // given
        int timeout = 20;
        Settings settings = mockSettings(true, timeout);
        String player = "patrick";
        SessionManager manager = new SessionManager(settings);
        Map<String, Long> sessions = getSessionsMap(manager);
        // Add session entry for player that just has expired
        sessions.put(player, System.currentTimeMillis() - 1000);

        // when
        boolean result = manager.hasSession(player);

        // then
        assertThat(result, equalTo(false));
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
        assertThat(getSessionsMap(manager), anEmptyMap());
    }

    @Test
    public void shouldPerformCleanup() {
        // given
        Settings settings = mockSettings(true, 1);
        SessionManager manager = new SessionManager(settings);
        Map<String, Long> sessions = getSessionsMap(manager);
        sessions.put("somebody", System.currentTimeMillis() - 123L);
        sessions.put("someone", System.currentTimeMillis() + 4040L);
        sessions.put("anyone", System.currentTimeMillis() - 1000L);
        sessions.put("everyone", System.currentTimeMillis() + 60000L);

        // when
        manager.performCleanup();

        // then
        assertThat(sessions, aMapWithSize(2));
        assertThat(sessions.keySet(), containsInAnyOrder("someone", "everyone"));
    }

    @Test
    public void shouldNotPerformCleanup() {
        // given
        Settings settings = mockSettings(false, 1);
        SessionManager manager = new SessionManager(settings);
        Map<String, Long> sessions = getSessionsMap(manager);
        sessions.put("somebody", System.currentTimeMillis() - 123L);
        sessions.put("someone", System.currentTimeMillis() + 4040L);
        sessions.put("anyone", System.currentTimeMillis() - 1000L);
        sessions.put("everyone", System.currentTimeMillis() + 60000L);

        // when
        manager.performCleanup();

        // then
        assertThat(sessions, aMapWithSize(4)); // map not changed -> no cleanup performed
    }

    private static Map<String, Long> getSessionsMap(SessionManager manager) {
        return ReflectionTestUtils.getFieldValue(SessionManager.class, manager, "sessions");
    }


    private static Settings mockSettings(boolean isEnabled, int sessionTimeout) {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.SESSIONS_ENABLED)).willReturn(isEnabled);
        given(settings.getProperty(PluginSettings.SESSIONS_TIMEOUT)).willReturn(sessionTimeout);
        return settings;
    }
}
