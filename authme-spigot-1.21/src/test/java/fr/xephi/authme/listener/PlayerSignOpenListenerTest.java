package fr.xephi.authme.listener;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSignOpenEvent;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyBoolean;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PlayerSignOpenListenerTest {

    @InjectMocks
    private PlayerSignOpenListener listener;

    @Mock
    private ListenerService listenerService;

    @Test
    public void shouldCancelEventForUnauthenticated() {
        // given
        // PlayerSignOpenEvent#getPlayer is final (PlayerEvent), so use spy on a real instance
        Player player = mock(Player.class);
        PlayerSignOpenEvent event = spy(new PlayerSignOpenEvent(
            player, mock(Sign.class), Side.FRONT, PlayerSignOpenEvent.Cause.UNKNOWN));
        given(listenerService.shouldCancelEvent(player)).willReturn(true);

        // when
        listener.onPlayerSignOpen(event);

        // then
        verify(event).setCancelled(true);
    }

    @Test
    public void shouldNotCancelEventForAuthenticatedPlayer() {
        // given
        Player player = mock(Player.class);
        PlayerSignOpenEvent event = spy(new PlayerSignOpenEvent(
            player, mock(Sign.class), Side.FRONT, PlayerSignOpenEvent.Cause.UNKNOWN));
        given(listenerService.shouldCancelEvent(player)).willReturn(false);

        // when
        listener.onPlayerSignOpen(event);

        // then
        verify(event, never()).setCancelled(anyBoolean());
    }
}


