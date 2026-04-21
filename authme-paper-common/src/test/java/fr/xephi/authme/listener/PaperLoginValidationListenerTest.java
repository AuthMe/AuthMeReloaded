package fr.xephi.authme.listener;

import com.destroystokyo.paper.profile.PlayerProfile;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import io.papermc.paper.event.player.PlayerServerFullCheckEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PaperLoginValidationListenerTest {

    @InjectMocks
    private PaperLoginValidationListener listener;

    @Mock
    private OnJoinVerifier onJoinVerifier;

    @Mock
    private Messages messages;

    @Test
    public void shouldIgnoreConfigurationStageValidationEvent() {
        PlayerConfigurationConnection connection = mock(PlayerConfigurationConnection.class);
        PlayerConnectionValidateLoginEvent event = new PlayerConnectionValidateLoginEvent(connection, null);

        listener.onPlayerConnectionValidateLogin(event);

        verifyNoInteractions(onJoinVerifier, messages);
        assertThat(event.isAllowed(), is(true));
    }

    @Test
    public void shouldKickDuplicateSessionOnValidationEvent() throws FailedVerificationException {
        PlayerLoginConnection connection = mock(PlayerLoginConnection.class);
        PlayerProfile profile = mock(PlayerProfile.class);
        given(connection.getAuthenticatedProfile()).willReturn(profile);
        given(profile.getName()).willReturn("Bobby");
        given(messages.retrieveSingle("Bobby", MessageKey.USERNAME_ALREADY_ONLINE_ERROR))
            .willReturn("&cAlready online");
        FailedVerificationException exception = new FailedVerificationException(MessageKey.USERNAME_ALREADY_ONLINE_ERROR);
        org.mockito.BDDMockito.willThrow(exception).given(onJoinVerifier).checkSingleSession("Bobby");
        PlayerConnectionValidateLoginEvent event = new PlayerConnectionValidateLoginEvent(connection, null);

        listener.onPlayerConnectionValidateLogin(event);

        assertThat(event.isAllowed(), is(false));
        assertThat(LegacyComponentSerializer.legacySection().serialize(event.getKickMessage()), is("&cAlready online"));
    }

    @Test
    public void shouldAllowServerFullEventWhenVerifierMakesRoom() {
        PlayerProfile profile = mock(PlayerProfile.class);
        given(profile.getName()).willReturn("VipPlayer");
        given(onJoinVerifier.getServerFullKickMessageIfDenied("VipPlayer")).willReturn(null);
        PlayerServerFullCheckEvent event = new PlayerServerFullCheckEvent(profile, Component.text("full"), true);

        listener.onPlayerServerFullCheck(event);

        assertThat(event.isAllowed(), is(true));
    }

    @Test
    public void shouldDenyServerFullEventWithVerifierMessage() {
        PlayerProfile profile = mock(PlayerProfile.class);
        given(profile.getName()).willReturn("RegularPlayer");
        given(onJoinVerifier.getServerFullKickMessageIfDenied("RegularPlayer")).willReturn("&cServer full");
        PlayerServerFullCheckEvent event = new PlayerServerFullCheckEvent(profile, Component.text("full"), true);

        listener.onPlayerServerFullCheck(event);

        assertThat(event.isAllowed(), is(false));
        assertThat(LegacyComponentSerializer.legacySection().serialize(event.kickMessage()), is("&cServer full"));
        verify(onJoinVerifier).getServerFullKickMessageIfDenied("RegularPlayer");
    }
}
