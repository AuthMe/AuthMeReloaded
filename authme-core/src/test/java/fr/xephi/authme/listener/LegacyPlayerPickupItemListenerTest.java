package fr.xephi.authme.listener;

import org.bukkit.event.player.PlayerPickupItemEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LegacyPlayerPickupItemListenerTest {

    @InjectMocks
    private LegacyPlayerPickupItemListener listener;

    @Mock
    private ListenerService listenerService;

    @Test
    void shouldCancelWhenShouldCancelEventReturnsTrue() {
        PlayerPickupItemEvent event = mock(PlayerPickupItemEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(true);

        listener.onLegacyPlayerPickupItem(event);

        verify(event).setCancelled(true);
    }

    @Test
    void shouldNotCancelWhenShouldCancelEventReturnsFalse() {
        PlayerPickupItemEvent event = mock(PlayerPickupItemEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(false);

        listener.onLegacyPlayerPickupItem(event);

        verifyNoInteractions(event);
    }
}
