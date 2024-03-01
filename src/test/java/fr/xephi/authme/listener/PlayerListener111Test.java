package fr.xephi.authme.listener;

import org.bukkit.event.entity.EntityAirChangeEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static fr.xephi.authme.listener.EventCancelVerifier.withServiceMock;

/**
 * Test for {@link PlayerListener111}.
 */
@ExtendWith(MockitoExtension.class)
class PlayerListener111Test {

    @InjectMocks
    private PlayerListener111 listener;

    @Mock
    private ListenerService listenerService;

    @Test
    void shouldCancelEvent() {
        withServiceMock(listenerService)
            .check(listener::onPlayerAirChange, EntityAirChangeEvent.class);
    }

}
