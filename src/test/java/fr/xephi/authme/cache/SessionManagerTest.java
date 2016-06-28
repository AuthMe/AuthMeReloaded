package fr.xephi.authme.cache;

import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

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
        BukkitTask task = mock(BukkitTask.class);

        // when
        manager.addSession(player, task);

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
    public void shouldAddSession() {
        // given
        NewSetting settings = mockSettings(true, 10);
        SessionManager manager = new SessionManager(settings);
        String player = "playah";
        BukkitTask task = mock(BukkitTask.class);

        // when
        manager.addSession(player, task);

        // then
        assertThat(manager.hasSession(player), equalTo(true));
    }

    @Test
    public void shouldNotAddSessionBecauseDisabled() {
        // given
        NewSetting settings = mockSettings(false, 10);
        SessionManager manager = new SessionManager(settings);
        String player = "playah";
        BukkitTask task = mock(BukkitTask.class);

        // when
        manager.addSession(player, task);

        // then
        assertThat(manager.hasSession(player), equalTo(false));
    }

    @Test
    public void shouldNotAddSessionBecauseTimeoutIsZero() {
        // given
        NewSetting settings = mockSettings(true, 0);
        SessionManager manager = new SessionManager(settings);
        String player = "playah";
        BukkitTask task = mock(BukkitTask.class);

        // when
        manager.addSession(player, task);

        // then
        assertThat(manager.hasSession(player), equalTo(false));
    }

    private static NewSetting mockSettings(boolean isEnabled, int sessionTimeout) {
        NewSetting settings = mock(NewSetting.class);
        given(settings.getProperty(PluginSettings.SESSIONS_ENABLED)).willReturn(isEnabled);
        given(settings.getProperty(PluginSettings.SESSIONS_TIMEOUT)).willReturn(sessionTimeout);
        return settings;
    }
}
