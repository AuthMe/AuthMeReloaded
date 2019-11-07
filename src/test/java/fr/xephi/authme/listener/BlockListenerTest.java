package fr.xephi.authme.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link BlockListener}.
 */
@ExtendWith(MockitoExtension.class)
class BlockListenerTest {

    @InjectMocks
    private BlockListener listener;

    @Mock
    private ListenerService listenerService;

    @Test
    void shouldAllowPlaceEvent() {
        // given
        Player player = mock(Player.class);
        BlockPlaceEvent event = mock(BlockPlaceEvent.class);
        given(event.getPlayer()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(false);

        // when
        listener.onBlockPlace(event);

        // then
        verify(event).getPlayer();
        verifyNoMoreInteractions(event);
    }

    @Test
    void shouldDenyPlaceEvent() {
        // given
        Player player = mock(Player.class);
        BlockPlaceEvent event = mock(BlockPlaceEvent.class);
        given(event.getPlayer()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(true);

        // when
        listener.onBlockPlace(event);

        // then
        verify(event).setCancelled(true);
        verify(event).getPlayer();
        verifyNoMoreInteractions(event);
    }

    @Test
    void shouldAllowBreakEvent() {
        // given
        Player player = mock(Player.class);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        given(event.getPlayer()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(false);

        // when
        listener.onBlockBreak(event);

        // then
        verify(event).getPlayer();
        verifyNoMoreInteractions(event);
    }

    @Test
    void shouldDenyBreakEvent() {
        // given
        Player player = mock(Player.class);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        given(event.getPlayer()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(true);

        // when
        listener.onBlockBreak(event);

        // then
        verify(event).setCancelled(true);
        verify(event).getPlayer();
        verifyNoMoreInteractions(event);
    }

}
