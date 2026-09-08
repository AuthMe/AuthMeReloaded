package fr.xephi.authme.listener;

import org.bukkit.event.entity.EntityAirChangeEvent;
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
class EntityAirChangeListenerTest {

    @InjectMocks
    private EntityAirChangeListener listener;

    @Mock
    private ListenerService listenerService;

    @Test
    void shouldCancelWhenShouldCancelEventReturnsTrue() {
        EntityAirChangeEvent event = mock(EntityAirChangeEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(true);

        listener.onPlayerAirChange(event);

        verify(event).setCancelled(true);
    }

    @Test
    void shouldNotCancelWhenShouldCancelEventReturnsFalse() {
        EntityAirChangeEvent event = mock(EntityAirChangeEvent.class);
        given(listenerService.shouldCancelEvent(event)).willReturn(false);

        listener.onPlayerAirChange(event);

        verifyNoInteractions(event);
    }
}
