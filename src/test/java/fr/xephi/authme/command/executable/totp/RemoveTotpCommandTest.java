package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.totp.TotpAuthenticator;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link RemoveTotpCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RemoveTotpCommandTest {

    @InjectMocks
    private RemoveTotpCommand command;

    @Mock
    private DataSource dataSource;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private TotpAuthenticator totpAuthenticator;
    @Mock
    private Messages messages;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldRemoveTotpKey() {
        // given
        String name = "aws";
        PlayerAuth auth = PlayerAuth.builder().name(name).totpKey("some-totp-key").build();
        given(playerCache.getAuth(name)).willReturn(auth);
        String inputCode = "93847";
        given(totpAuthenticator.checkCode(auth, inputCode)).willReturn(true);
        given(dataSource.removeTotpKey(name)).willReturn(true);
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);

        // when
        command.runCommand(player, singletonList(inputCode));

        // then
        verify(dataSource).removeTotpKey(name);
        verify(messages, only()).send(player, MessageKey.TWO_FACTOR_REMOVED_SUCCESS);
        verify(playerCache).updatePlayer(auth);
        assertThat(auth.getTotpKey(), nullValue());
    }

    @Test
    public void shouldHandleDatabaseError() {
        // given
        String name = "aws";
        PlayerAuth auth = PlayerAuth.builder().name(name).totpKey("some-totp-key").build();
        given(playerCache.getAuth(name)).willReturn(auth);
        String inputCode = "93847";
        given(totpAuthenticator.checkCode(auth, inputCode)).willReturn(true);
        given(dataSource.removeTotpKey(name)).willReturn(false);
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);

        // when
        command.runCommand(player, singletonList(inputCode));

        // then
        verify(dataSource).removeTotpKey(name);
        verify(messages, only()).send(player, MessageKey.ERROR);
        verify(playerCache, only()).getAuth(name);
    }

    @Test
    public void shouldHandleInvalidCode() {
        // given
        String name = "cesar";
        PlayerAuth auth = PlayerAuth.builder().name(name).totpKey("some-totp-key").build();
        given(playerCache.getAuth(name)).willReturn(auth);
        String inputCode = "93847";
        given(totpAuthenticator.checkCode(auth, inputCode)).willReturn(false);
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);

        // when
        command.runCommand(player, singletonList(inputCode));

        // then
        verifyNoInteractions(dataSource);
        verify(messages, only()).send(player, MessageKey.TWO_FACTOR_INVALID_CODE);
        verify(playerCache, only()).getAuth(name);
    }

    @Test
    public void shouldHandleUserWithoutTotpKey() {
        // given
        String name = "cesar";
        PlayerAuth auth = PlayerAuth.builder().name(name).build();
        given(playerCache.getAuth(name)).willReturn(auth);
        String inputCode = "654684";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);

        // when
        command.runCommand(player, singletonList(inputCode));

        // then
        verifyNoInteractions(dataSource, totpAuthenticator);
        verify(messages, only()).send(player, MessageKey.TWO_FACTOR_NOT_ENABLED_ERROR);
        verify(playerCache, only()).getAuth(name);
    }

    @Test
    public void shouldHandleNonLoggedInUser() {
        // given
        String name = "cesar";
        given(playerCache.getAuth(name)).willReturn(null);
        String inputCode = "654684";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);

        // when
        command.runCommand(player, singletonList(inputCode));

        // then
        verifyNoInteractions(dataSource, totpAuthenticator);
        verify(messages, only()).send(player, MessageKey.NOT_LOGGED_IN);
        verify(playerCache, only()).getAuth(name);
    }
}
