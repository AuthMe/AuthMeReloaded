package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link ConfirmTotpCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfirmTotpCommandTest {

    @InjectMocks
    private ConfirmTotpCommand command;

    @Mock
    private GenerateTotpService generateTotpService;
    @Mock
    private DataSource dataSource;
    @Mock
    private Messages messages;

    @Test
    public void shouldAddTotpCodeToUserAfterSuccessfulConfirmation() {
        // given
        Player player = mock(Player.class);
        String playerName = "George";
        given(player.getName()).willReturn(playerName);
        PlayerAuth auth = PlayerAuth.builder().name(playerName).build();
        given(dataSource.getAuth(playerName)).willReturn(auth);
        given(generateTotpService.getGeneratedTotpKey(player)).willReturn(new TotpGenerationResult("totp-key", "url-not-relevant"));
        String totpCode = "954321";
        given(generateTotpService.isTotpCodeCorrectForGeneratedTotpKey(player, totpCode)).willReturn(true);

        // when
        command.runCommand(player, Collections.singletonList(totpCode));

        // then
        verify(generateTotpService).isTotpCodeCorrectForGeneratedTotpKey(player, totpCode);
        verify(generateTotpService).removeGenerateTotpKey(player);
        verify(dataSource).setTotpKey(playerName, "totp-key");
        verify(messages).send(player, MessageKey.TWO_FACTOR_ENABLE_SUCCESS);
    }

    @Test
    public void shouldHandleWrongTotpCode() {
        // given
        Player player = mock(Player.class);
        String playerName = "George";
        given(player.getName()).willReturn(playerName);
        PlayerAuth auth = PlayerAuth.builder().name(playerName).build();
        given(dataSource.getAuth(playerName)).willReturn(auth);
        given(generateTotpService.getGeneratedTotpKey(player)).willReturn(new TotpGenerationResult("totp-key", "url-not-relevant"));
        String totpCode = "754321";
        given(generateTotpService.isTotpCodeCorrectForGeneratedTotpKey(player, totpCode)).willReturn(false);

        // when
        command.runCommand(player, Collections.singletonList(totpCode));

        // then
        verify(generateTotpService).isTotpCodeCorrectForGeneratedTotpKey(player, totpCode);
        verify(generateTotpService, never()).removeGenerateTotpKey(any(Player.class));
        verify(dataSource, only()).getAuth(playerName);
        verify(messages).send(player, MessageKey.TWO_FACTOR_ENABLE_ERROR_WRONG_CODE);
    }

    @Test
    public void shouldHandleMissingTotpKey() {
        // given
        Player player = mock(Player.class);
        String playerName = "George";
        given(player.getName()).willReturn(playerName);
        PlayerAuth auth = PlayerAuth.builder().name(playerName).build();
        given(dataSource.getAuth(playerName)).willReturn(auth);
        given(generateTotpService.getGeneratedTotpKey(player)).willReturn(null);

        // when
        command.runCommand(player, Collections.singletonList("871634"));

        // then
        verify(generateTotpService, only()).getGeneratedTotpKey(player);
        verify(dataSource, only()).getAuth(playerName);
        verify(messages).send(player, MessageKey.TWO_FACTOR_ENABLE_ERROR_NO_CODE);
    }

    @Test
    public void shouldStopForAlreadyExistingTotpKeyOnAccount() {
        // given
        Player player = mock(Player.class);
        String playerName = "George";
        given(player.getName()).willReturn(playerName);
        PlayerAuth auth = PlayerAuth.builder().name(playerName).totpKey("A987234").build();
        given(dataSource.getAuth(playerName)).willReturn(auth);

        // when
        command.runCommand(player, Collections.singletonList("871634"));

        // then
        verify(dataSource, only()).getAuth(playerName);
        verifyZeroInteractions(generateTotpService);
        verify(messages).send(player, MessageKey.TWO_FACTOR_ALREADY_ENABLED);
    }

    @Test
    public void shouldHandleMissingAuthAccount() {
        // given
        Player player = mock(Player.class);
        String playerName = "George";
        given(player.getName()).willReturn(playerName);
        given(dataSource.getAuth(playerName)).willReturn(null);

        // when
        command.runCommand(player, Collections.singletonList("984685"));

        // then
        verify(dataSource, only()).getAuth(playerName);
        verifyZeroInteractions(generateTotpService);
        verify(messages).send(player, MessageKey.REGISTER_MESSAGE);
    }
}
