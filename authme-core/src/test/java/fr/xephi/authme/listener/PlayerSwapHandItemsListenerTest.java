package fr.xephi.authme.listener;

import org.bukkit.event.player.PlayerSwapHandItemsEvent;
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
class PlayerSwapHandItemsListenerTest {

    @InjectMocks
    private PlayerSwapHandItemsListener listener;

    @Mock
    private ListenerService listenerService;

    @Test
    void shouldCancelWhenShouldCancelEventReturnsTrue() {
        PlayerSwapHandItemsEvent event = mock(PlayerSwapHandItemsEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(true);

        listener.onPlayerSwapHandItems(event);

        verify(event).setCancelled(true);
    }

    @Test
    void shouldNotCancelWhenShouldCancelEventReturnsFalse() {
        PlayerSwapHandItemsEvent event = mock(PlayerSwapHandItemsEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(false);

        listener.onPlayerSwapHandItems(event);

        verifyNoInteractions(event);
    }
}
