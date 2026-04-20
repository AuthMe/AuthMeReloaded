package fr.xephi.authme.listener;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static fr.xephi.authme.listener.EventCancelVerifier.withServiceMock;

/**
 * Test for {@link PlayerListener19}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PlayerListener19Test {

    @InjectMocks
    private PlayerListener19 listener;

    @Mock
    private ListenerService listenerService;

    @Test
    public void shouldCancelEvent() {
        withServiceMock(listenerService)
            .check(listener::onPlayerSwapHandItems, PlayerSwapHandItemsEvent.class);
    }

}


