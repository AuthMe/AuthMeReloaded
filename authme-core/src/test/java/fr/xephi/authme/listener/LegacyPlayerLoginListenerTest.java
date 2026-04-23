package fr.xephi.authme.listener;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.ValidationService;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class LegacyPlayerLoginListenerTest {

    @InjectMocks
    private LegacyPlayerLoginListener listener;

    @Mock
    private Messages messages;
    @Mock
    private OnJoinVerifier onJoinVerifier;
    @Mock
    private ValidationService validationService;

    @Test
    void shouldNotInterfereWithUnrestrictedUser() throws FailedVerificationException {
        String name = "Player01";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent event = spy(new PlayerLoginEvent(player, "", null));
        given(validationService.isUnrestricted(name)).willReturn(true);

        listener.onPlayerLogin(event);

        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).checkSingleSession(name);
        verifyNoModifyingCalls(event);
        verifyNoMoreInteractions(onJoinVerifier);
    }

    @Test
    void shouldStopHandlingForFullServer() throws FailedVerificationException {
        String name = "someone";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent event = spy(new PlayerLoginEvent(player, "", null));
        given(validationService.isUnrestricted(name)).willReturn(false);
        given(onJoinVerifier.refusePlayerForFullServer(event)).willReturn(true);

        listener.onPlayerLogin(event);

        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).checkSingleSession(name);
        verify(onJoinVerifier).refusePlayerForFullServer(event);
        verifyNoMoreInteractions(onJoinVerifier);
        verifyNoModifyingCalls(event);
    }

    @Test
    void shouldContinueHandlingAlreadyRejectedEvent() throws FailedVerificationException {
        String name = "someone";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent event = new PlayerLoginEvent(player, "", null);
        event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
        event = spy(event);
        given(validationService.isUnrestricted(name)).willReturn(false);
        given(onJoinVerifier.refusePlayerForFullServer(event)).willReturn(false);

        listener.onPlayerLogin(event);

        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).checkSingleSession(name);
        verify(onJoinVerifier).refusePlayerForFullServer(event);
        verifyNoModifyingCalls(event);
    }

    @Test
    void shouldKickForFailedSingleSessionCheck() throws FailedVerificationException {
        String name = "someone";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent event = spy(new PlayerLoginEvent(player, "", null));
        FailedVerificationException exception = new FailedVerificationException(MessageKey.ALREADY_LOGGED_IN_ERROR);
        doThrow(exception).when(onJoinVerifier).checkSingleSession(name);
        given(messages.retrieveSingle(name, exception.getReason(), exception.getArgs())).willReturn("Already logged in");

        listener.onPlayerLogin(event);

        verify(event).setKickMessage("Already logged in");
        verify(event).setResult(PlayerLoginEvent.Result.KICK_OTHER);
        verifyNoInteractions(validationService);
    }

    @Test
    void shouldPerformAllJoinVerificationsSuccessfullyLogin() {
        String name = "someone";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent loginEvent = spy(new PlayerLoginEvent(player, "", null));
        given(validationService.isUnrestricted(name)).willReturn(false);
        given(onJoinVerifier.refusePlayerForFullServer(loginEvent)).willReturn(false);

        listener.onPlayerLogin(loginEvent);

        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).refusePlayerForFullServer(loginEvent);
        verifyNoModifyingCalls(loginEvent);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }

    private static void verifyNoModifyingCalls(PlayerLoginEvent event) {
        verify(event, atLeast(0)).getPlayer();
        verify(event, atLeast(0)).getResult();
        verify(event, atLeast(0)).getAddress();
        verifyNoMoreInteractions(event);
    }
}
