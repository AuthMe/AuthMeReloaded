package fr.xephi.authme.listener;

import org.bukkit.event.player.PlayerEditBookEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test for {@link PlayerListener16}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PlayerListener16Test {

    @InjectMocks
    private PlayerListener16 listener;

    @Mock
    private ListenerService listenerService;

    @Test
    public void shouldCancelEvent() {
        ListenerTestUtils.checkEventIsCanceledForUnauthed(listener, listenerService, PlayerEditBookEvent.class);
    }

}
