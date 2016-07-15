package fr.xephi.authme.cache;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

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
        NewSetting settings = mockSettings(true, 10);
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
        NewSetting settings = mockSettings(true, 10);
        SessionManager manager = new SessionManager(settings);
        String player = "playah";

        // when/then
        assertThat(manager.hasSession(player), equalTo(false));
    }

    @Test
    public void shouldNotAddSessionBecauseDisabled() {
        // given
        NewSetting settings = mockSettings(false, 10);
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
        NewSetting settings = mockSettings(true, 0);
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
        NewSetting settings = mockSettings(true, 10);
        String player = "user";
        SessionManager manager = new SessionManager(settings);
        manager.addSession(player);

        // when
        manager.removeSession(player);

        // then
        assertThat(manager.hasSession(player), equalTo(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldDenySessionIfTimeoutHasExpired() {
        // given
        int timeout = 20;
        NewSetting settings = mockSettings(true, timeout);
        String player = "patrick";
        SessionManager manager = new SessionManager(settings);
        Map<String, Long> sessions = (Map<String, Long>) ReflectionTestUtils
            .getFieldValue(SessionManager.class, manager, "sessions");
        // Add session entry for player that just has expired
        sessions.put(player, System.currentTimeMillis() - 1000);

        // when
        boolean result = manager.hasSession(player);

        // then
        assertThat(result, equalTo(false));
    }

    private static NewSetting mockSettings(boolean isEnabled, int sessionTimeout) {
        NewSetting settings = mock(NewSetting.class);
        given(settings.getProperty(PluginSettings.SESSIONS_ENABLED)).willReturn(isEnabled);
        given(settings.getProperty(PluginSettings.SESSIONS_TIMEOUT)).willReturn(sessionTimeout);
        return settings;
    }
}
