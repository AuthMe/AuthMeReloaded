package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.totp.GenerateTotpService;
import fr.xephi.authme.security.totp.TotpAuthenticator.TotpGenerationResult;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
    private PlayerCache playerCache;
    @Mock
    private Messages messages;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldAddTotpCodeToUserAfterSuccessfulConfirmation() {
        // given
        Player player = mock(Player.class);
        String playerName = "George";
        given(player.getName()).willReturn(playerName);
        PlayerAuth auth = PlayerAuth.builder().name(playerName).build();
        given(playerCache.getAuth(playerName)).willReturn(auth);
        String generatedTotpKey = "totp-key";
        given(generateTotpService.getGeneratedTotpKey(player)).willReturn(new TotpGenerationResult(generatedTotpKey, "url-not-relevant"));
        String totpCode = "954321";
        given(generateTotpService.isTotpCodeCorrectForGeneratedTotpKey(player, totpCode)).willReturn(true);
        given(dataSource.setTotpKey(anyString(), anyString())).willReturn(true);

        // when
        command.runCommand(player, Collections.singletonList(totpCode));

        // then
        verify(generateTotpService).isTotpCodeCorrectForGeneratedTotpKey(player, totpCode);
        verify(generateTotpService).removeGenerateTotpKey(player);
        verify(dataSource).setTotpKey(playerName, generatedTotpKey);
        verify(playerCache).updatePlayer(auth);
        verify(messages).send(player, MessageKey.TWO_FACTOR_ENABLE_SUCCESS);
        assertThat(auth.getTotpKey(), equalTo(generatedTotpKey));
    }

    @Test
    public void shouldHandleWrongTotpCode() {
        // given
        Player player = mock(Player.class);
        String playerName = "George";
        given(player.getName()).willReturn(playerName);
        PlayerAuth auth = PlayerAuth.builder().name(playerName).build();
        given(playerCache.getAuth(playerName)).willReturn(auth);
        given(generateTotpService.getGeneratedTotpKey(player)).willReturn(new TotpGenerationResult("totp-key", "url-not-relevant"));
        String totpCode = "754321";
        given(generateTotpService.isTotpCodeCorrectForGeneratedTotpKey(player, totpCode)).willReturn(false);

        // when
        command.runCommand(player, Collections.singletonList(totpCode));

        // then
        verify(generateTotpService).isTotpCodeCorrectForGeneratedTotpKey(player, totpCode);
        verify(generateTotpService, never()).removeGenerateTotpKey(any(Player.class));
        verify(playerCache, only()).getAuth(playerName);
        verify(messages).send(player, MessageKey.TWO_FACTOR_ENABLE_ERROR_WRONG_CODE);
        verifyNoInteractions(dataSource);
    }

    @Test
    public void shouldHandleMissingTotpKey() {
        // given
        Player player = mock(Player.class);
        String playerName = "George";
        given(player.getName()).willReturn(playerName);
        PlayerAuth auth = PlayerAuth.builder().name(playerName).build();
        given(playerCache.getAuth(playerName)).willReturn(auth);
        given(generateTotpService.getGeneratedTotpKey(player)).willReturn(null);

        // when
        command.runCommand(player, Collections.singletonList("871634"));

        // then
        verify(generateTotpService, only()).getGeneratedTotpKey(player);
        verify(playerCache, only()).getAuth(playerName);
        verify(messages).send(player, MessageKey.TWO_FACTOR_ENABLE_ERROR_NO_CODE);
        verifyNoInteractions(dataSource);
    }

    @Test
    public void shouldStopForAlreadyExistingTotpKeyOnAccount() {
        // given
        Player player = mock(Player.class);
        String playerName = "George";
        given(player.getName()).willReturn(playerName);
        PlayerAuth auth = PlayerAuth.builder().name(playerName).totpKey("A987234").build();
        given(playerCache.getAuth(playerName)).willReturn(auth);

        // when
        command.runCommand(player, Collections.singletonList("871634"));

        // then
        verify(playerCache, only()).getAuth(playerName);
        verifyNoInteractions(generateTotpService, dataSource);
        verify(messages).send(player, MessageKey.TWO_FACTOR_ALREADY_ENABLED);
    }

    @Test
    public void shouldHandleMissingAuthAccount() {
        // given
        Player player = mock(Player.class);
        String playerName = "George";
        given(player.getName()).willReturn(playerName);
        given(playerCache.getAuth(playerName)).willReturn(null);

        // when
        command.runCommand(player, Collections.singletonList("984685"));

        // then
        verify(playerCache, only()).getAuth(playerName);
        verifyNoInteractions(generateTotpService, dataSource);
        verify(messages).send(player, MessageKey.NOT_LOGGED_IN);
    }
}
