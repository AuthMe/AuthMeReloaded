package fr.xephi.authme.listener;

import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test for {@link PlayerListener18}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PlayerListener18Test {

    @InjectMocks
    private PlayerListener18 listener;

    @Mock
    private ListenerService listenerService;

    @Test
    public void shouldCancelEvent() {
        ListenerTestUtils.checkEventIsCanceledForUnauthed(listener, listenerService, PlayerInteractAtEntityEvent.class);
    }

}
