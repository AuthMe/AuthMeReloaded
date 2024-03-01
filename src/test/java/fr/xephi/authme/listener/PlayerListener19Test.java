package fr.xephi.authme.listener;

import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static fr.xephi.authme.listener.EventCancelVerifier.withServiceMock;

/**
 * Test for {@link PlayerListener19}.
 */
@ExtendWith(MockitoExtension.class)
class PlayerListener19Test {

    @InjectMocks
    private PlayerListener19 listener;

    @Mock
    private ListenerService listenerService;

    @Test
    void shouldCancelEvent() {
        withServiceMock(listenerService)
            .check(listener::onPlayerSwapHandItems, PlayerSwapHandItemsEvent.class);
    }

}
