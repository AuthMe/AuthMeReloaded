package fr.xephi.authme.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EntityPickupItemListenerTest {

    @InjectMocks
    private EntityPickupItemListener listener;

    @Mock
    private ListenerService listenerService;

    @Test
    void shouldIgnoreNonPlayerEntity() {
        EntityPickupItemEvent event = mock(EntityPickupItemEvent.class);
        given(event.getEntity()).willReturn(mock(LivingEntity.class));

        listener.onPlayerPickupItem(event);

        verifyNoInteractions(listenerService);
        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    void shouldCancelForPlayerWhenShouldCancelTrue() {
        Player player = mock(Player.class);
        EntityPickupItemEvent event = mock(EntityPickupItemEvent.class);
        given(event.getEntity()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(true);

        listener.onPlayerPickupItem(event);

        verify(event).setCancelled(true);
    }

    @Test
    void shouldNotCancelForPlayerWhenShouldCancelFalse() {
        Player player = mock(Player.class);
        EntityPickupItemEvent event = mock(EntityPickupItemEvent.class);
        given(event.getEntity()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(false);

        listener.onPlayerPickupItem(event);

        verify(event, never()).setCancelled(anyBoolean());
    }
}
