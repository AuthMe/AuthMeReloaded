package fr.xephi.authme.listener;

import com.destroystokyo.paper.profile.PlayerProfile;
import fr.xephi.authme.service.ValidationService;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import io.papermc.paper.event.player.PlayerServerFullCheckEvent;
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
 * Test for {@link PlayerListenerPost1217Test}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PlayerListenerPost1217Test {

    @InjectMocks
    private PlayerListenerPost1217 listener;

    @Mock
    private OnJoinVerifier onJoinVerifier;
    @Mock
    private ValidationService validationService;

    @Test
    public void shouldStopHandlingForFullServer() throws FailedVerificationException {
        // given
        var name = "someone";
        var profile = mockPlayerProfileWithName(name);
        var event = spy(new PlayerServerFullCheckEvent(profile, null, false));
        given(onJoinVerifier.refusePlayerForFullServer(event)).willReturn(true);

        // when
        listener.onPlayerServerFullCheckEvent(event);

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
        var name = "someone";
        var connection = mockPlayerLoginConnectionWithName(name);
        var event = new PlayerLoginEvent(player, "", null);
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

    private static PlayerLoginConnection mockPlayerLoginConnectionWithName(String name) {
        var connection = mock(PlayerLoginConnection.class);
        var profile = mockPlayerProfileWithName(name);
        given(connection.getUnsafeProfile()).willReturn(profile);
        return connection;
    }

    private static PlayerProfile mockPlayerProfileWithName(String name) {
        var profile = mock(PlayerProfile.class);
        given(profile.getName()).willReturn(name);
        return profile;
    }

    private static void verifyNoModifyingCalls(PlayerConnectionValidateLoginEvent event) {
        verify(event, atLeast(0)).getConnection();
        verify(event, atLeast(0)).getKickMessage();
        verifyNoMoreInteractions(event);
    }
}
