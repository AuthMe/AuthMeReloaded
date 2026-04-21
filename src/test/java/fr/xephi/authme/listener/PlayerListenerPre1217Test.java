package fr.xephi.authme.listener;

import fr.xephi.authme.service.ValidationService;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link PlayerListenerPre1217Test}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PlayerListenerPre1217Test {

    @InjectMocks
    private PlayerListenerPre1217 listener;

    @Mock
    private OnJoinVerifier onJoinVerifier;
    @Mock
    private ValidationService validationService;

    @Test
    public void shouldNotInterfereWithUnrestrictedUser() throws FailedVerificationException {
        // given
        String name = "Player01";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent event = spy(new PlayerLoginEvent(player, "", null));
        given(validationService.isUnrestricted(name)).willReturn(true);

        // when
        listener.onPlayerLogin(event);

        // then
        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).checkSingleSession(name);
        verifyNoModifyingCalls(event);
        verifyNoMoreInteractions(onJoinVerifier);
    }

    @Test
    public void shouldStopHandlingForFullServer() throws FailedVerificationException {
        // given
        String name = "someone";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent event = spy(new PlayerLoginEvent(player, "", null));
        given(validationService.isUnrestricted(name)).willReturn(false);
        given(onJoinVerifier.refusePlayerForFullServer(event)).willReturn(true);

        // when
        listener.onPlayerLogin(event);

        // then
        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).checkSingleSession(name);
        verify(onJoinVerifier).refusePlayerForFullServer(event);
        verifyNoMoreInteractions(onJoinVerifier);
        verifyNoModifyingCalls(event);
    }

    @Test
    public void shouldStopHandlingEventForBadResult() throws FailedVerificationException {
        // given
        String name = "someone";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent event = new PlayerLoginEvent(player, "", null);
        event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
        event = spy(event);
        given(validationService.isUnrestricted(name)).willReturn(false);
        given(onJoinVerifier.refusePlayerForFullServer(event)).willReturn(false);

        // when
        listener.onPlayerLogin(event);

        // then
        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).checkSingleSession(name);
        verify(onJoinVerifier).refusePlayerForFullServer(event);
        verifyNoModifyingCalls(event);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }

    private static void verifyNoModifyingCalls(PlayerLoginEvent event) {
        verify(event, atLeast(0)).getResult();
        verify(event, atLeast(0)).getAddress();
        verifyNoMoreInteractions(event);
    }
}
