package fr.xephi.authme.command.executable.unregister;

import fr.xephi.authme.data.VerificationCodeManager;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.CommonService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link UnregisterCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class UnregisterCommandTest {

    @InjectMocks
    private UnregisterCommand command;

    @Mock
    private Management management;

    @Mock
    private CommonService commonService;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private VerificationCodeManager codeManager;

    @Test
    public void shouldCatchUnauthenticatedUser() {
        // given
        String password = "mySecret123";
        String name = "player77";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);

        // when
        command.executeCommand(player, Collections.singletonList(password));

        // then
        verify(playerCache).isAuthenticated(name);
        verify(commonService).send(player, MessageKey.NOT_LOGGED_IN);
        verifyNoInteractions(management);
    }

    @Test
    public void shouldStopForMissingVerificationCode() {
        // given
        String name = "asldjf";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        given(playerCache.isAuthenticated(name)).willReturn(true);
        given(codeManager.isVerificationRequired(player)).willReturn(true);

        // when
        command.executeCommand(player, Collections.singletonList("blergh"));

        // then
        verify(playerCache).isAuthenticated(name);
        verify(codeManager).codeExistOrGenerateNew(name);
        verify(commonService).send(player, MessageKey.VERIFICATION_CODE_REQUIRED);
        verifyNoInteractions(management);
    }

    @Test
    public void shouldForwardDataToAsyncTask() {
        // given
        String password = "p@ssw0rD";
        String name = "jas0n_";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        given(playerCache.isAuthenticated(name)).willReturn(true);
        given(codeManager.isVerificationRequired(player)).willReturn(false);

        // when
        command.executeCommand(player, Collections.singletonList(password));

        // then
        verify(playerCache).isAuthenticated(name);
        verify(management).performUnregister(player, password);
        verify(codeManager).isVerificationRequired(player);
    }

    @Test
    public void shouldStopIfSenderIsNotPlayer() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList("password"));

        // then
        verifyNoInteractions(playerCache, management);
        verify(sender).sendMessage(argThat(containsString("/authme unregister <player>")));
    }

    @Test
    public void shouldDefineArgumentMismatchMessage() {
        // given / when / then
        assertThat(command.getArgumentsMismatchMessage(), equalTo(MessageKey.USAGE_UNREGISTER));
    }
}
