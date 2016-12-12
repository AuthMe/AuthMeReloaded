package fr.xephi.authme.listener;

import org.bukkit.event.entity.EntityAirChangeEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static fr.xephi.authme.listener.EventCancelVerifier.withServiceMock;

/**
 * Test for {@link PlayerListener111}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PlayerListener111Test {

    @InjectMocks
    private PlayerListener111 listener;

    @Mock
    private ListenerService listenerService;

    @Test
    public void shouldCancelEvent() {
        withServiceMock(listenerService)
            .check(listener::onPlayerAirChange, EntityAirChangeEvent.class);
    }

}
