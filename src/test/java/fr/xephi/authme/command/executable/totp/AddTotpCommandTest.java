package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.totp.GenerateTotpService;
import fr.xephi.authme.security.totp.TotpAuthenticator.TotpGenerationResult;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link AddTotpCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AddTotpCommandTest {

    @InjectMocks
    private AddTotpCommand addTotpCommand;

    @Mock
    private GenerateTotpService generateTotpService;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private Messages messages;

    @Test
    public void shouldHandleNonLoggedInUser() {
        // given
        Player player = mockPlayerWithName("bob");
        given(playerCache.getAuth("bob")).willReturn(null);

        // when
        addTotpCommand.runCommand(player, Collections.emptyList());

        // then
        verify(messages).send(player, MessageKey.NOT_LOGGED_IN);
        verifyZeroInteractions(generateTotpService);
    }

    @Test
    public void shouldNotAddCodeForAlreadyExistingTotp() {
        // given
        Player player = mockPlayerWithName("arend");
        PlayerAuth auth = PlayerAuth.builder().name("arend")
            .totpKey("TOTP2345").build();
        given(playerCache.getAuth("arend")).willReturn(auth);

        // when
        addTotpCommand.runCommand(player, Collections.emptyList());

        // then
        verify(messages).send(player, MessageKey.TWO_FACTOR_ALREADY_ENABLED);
        verifyZeroInteractions(generateTotpService);
    }

    @Test
    public void shouldGenerateTotpCode() {
        // given
        Player player = mockPlayerWithName("charles");
        PlayerAuth auth = PlayerAuth.builder().name("charles").build();
        given(playerCache.getAuth("charles")).willReturn(auth);

        TotpGenerationResult generationResult = new TotpGenerationResult(
            "777Key214", "http://example.org/qr-code/link");
        given(generateTotpService.generateTotpKey(player)).willReturn(generationResult);

        // when
        addTotpCommand.runCommand(player, Collections.emptyList());

        // then
        verify(messages).send(player, MessageKey.TWO_FACTOR_CREATE, generationResult.getTotpKey(), generationResult.getAuthenticatorQrCodeUrl());
        verify(messages).send(player, MessageKey.TWO_FACTOR_CREATE_CONFIRMATION_REQUIRED);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }
}
