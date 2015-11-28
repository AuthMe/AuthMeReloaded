package fr.xephi.authme;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.WrapperMock;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link AntiBot}.
 */
public class AntiBotTest {

    private WrapperMock wrapper;

    @Before
    public void setUpMocks() {
        wrapper = WrapperMock.createInstance();
    }

    @Test
    public void shouldNotEnableAntiBot() {
        // given
        Settings.enableAntiBot = false;

        // when
        AntiBot.setupAntiBotService();

        // then
        verify(wrapper.getScheduler(), never()).scheduleSyncDelayedTask(any(AuthMe.class), any(Runnable.class));
    }
}
