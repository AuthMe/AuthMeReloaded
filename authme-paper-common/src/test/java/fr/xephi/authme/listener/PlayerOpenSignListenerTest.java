package fr.xephi.authme.listener;

import io.papermc.paper.event.player.PlayerOpenSignEvent;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PlayerOpenSignListenerTest {

    @InjectMocks
    private PlayerOpenSignListener listener;

    @Mock
    private ListenerService listenerService;

    @Test
    public void shouldCancelEventForUnauthenticated() {
        // given
        // PlayerOpenSignEvent#getPlayer is final (PlayerEvent), so use spy on a real instance
        Player player = mock(Player.class);
        PlayerOpenSignEvent event = spy(new PlayerOpenSignEvent(
            player, mock(Sign.class), Side.FRONT, PlayerOpenSignEvent.Cause.UNKNOWN));
        given(listenerService.shouldCancelEvent(player)).willReturn(true);

        // when
        listener.onPlayerOpenSign(event);

        verify(event).setCancelled(true);
    }

    @Test
    public void shouldNotCancelEventForAuthenticatedPlayer() {
        Player player = mock(Player.class);
        PlayerOpenSignEvent event = spy(new PlayerOpenSignEvent(
            player, mock(Sign.class), Side.FRONT, PlayerOpenSignEvent.Cause.UNKNOWN));
        given(listenerService.shouldCancelEvent(player)).willReturn(false);

        // when
        listener.onPlayerOpenSign(event);

        verify(event, never()).setCancelled(anyBoolean());
    }
}
