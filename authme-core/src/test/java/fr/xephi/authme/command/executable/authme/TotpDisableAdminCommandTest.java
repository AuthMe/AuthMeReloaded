package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link TotpDisableAdminCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class TotpDisableAdminCommandTest {

    @InjectMocks
    private TotpDisableAdminCommand command;

    @Mock
    private DataSource dataSource;

    @Mock
    private Messages messages;

    @Mock
    private BukkitService bukkitService;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldHandleUnknownUser() {
        // given
        CommandSender sender = mock(CommandSender.class);
        given(dataSource.getAuth("user")).willReturn(null);

        // when
        command.executeCommand(sender, Collections.singletonList("user"));

        // then
        verify(messages).send(sender, MessageKey.UNKNOWN_USER);
        verify(dataSource, only()).getAuth("user");
    }

    @Test
    public void shouldHandleUserWithNoTotpEnabled() {
        // given
        CommandSender sender = mock(CommandSender.class);
        PlayerAuth auth = PlayerAuth.builder()
            .name("billy")
            .totpKey(null)
            .build();
        given(dataSource.getAuth("Billy")).willReturn(auth);

        // when
        command.executeCommand(sender, Collections.singletonList("Billy"));

        // then
        verify(sender).sendMessage(argThat(containsString("'Billy' does not have two-factor auth enabled")));
        verify(dataSource, only()).getAuth("Billy");
    }

    @Test
    public void shouldRemoveTotpFromUser() {
        // given
        CommandSender sender = mock(CommandSender.class);
        PlayerAuth auth = PlayerAuth.builder()
            .name("Bobby")
            .totpKey("56484998")
            .build();
        given(dataSource.getAuth("Bobby")).willReturn(auth);
        given(dataSource.removeTotpKey("Bobby")).willReturn(true);
        Player player = mock(Player.class);
        given(bukkitService.getPlayerExact("Bobby")).willReturn(player);

        // when
        command.executeCommand(sender, Collections.singletonList("Bobby"));

        // then
        verify(sender).sendMessage(argThat(containsString("Disabled two-factor authentication successfully")));
        verify(messages).send(player, MessageKey.TWO_FACTOR_REMOVED_SUCCESS);
    }

    @Test
    public void shouldHandleErrorWhileRemovingTotp() {
        // given
        CommandSender sender = mock(CommandSender.class);
        PlayerAuth auth = PlayerAuth.builder()
            .name("Bobby")
            .totpKey("321654")
            .build();
        given(dataSource.getAuth("Bobby")).willReturn(auth);
        given(dataSource.removeTotpKey("Bobby")).willReturn(false);

        // when
        command.executeCommand(sender, Collections.singletonList("Bobby"));

        // then
        verify(messages).send(sender, MessageKey.ERROR);
    }
}
