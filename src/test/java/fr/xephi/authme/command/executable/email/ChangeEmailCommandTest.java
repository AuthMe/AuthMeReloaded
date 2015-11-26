package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.AuthMeMockUtil;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.process.Management;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ChangeEmailCommand}.
 */
public class ChangeEmailCommandTest {

    private AuthMe authMeMock;
    private Management managementMock;

    @Before
    public void setUpMocks() {
        AuthMeMockUtil.mockAuthMeInstance();
        authMeMock = AuthMe.getInstance();
        managementMock = Mockito.mock(Management.class);
        when(authMeMock.getManagement()).thenReturn(managementMock);
    }

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = Mockito.mock(BlockCommandSender.class);
        ChangeEmailCommand command = new ChangeEmailCommand();

        // when
        command.executeCommand(sender, new CommandParts(), new CommandParts());

        // then
        verify(authMeMock, never()).getManagement();
    }

    @Test
    public void shouldForwardData() {
        // given
        Player sender = Mockito.mock(Player.class);
        ChangeEmailCommand command = new ChangeEmailCommand();

        // when
        command.executeCommand(sender, new CommandParts(),
                new CommandParts(Arrays.asList("new.mail@example.org", "old_mail@example.org")));

        // then
        verify(authMeMock).getManagement();
        verify(managementMock).performChangeEmail(sender, "new.mail@example.org", "old_mail@example.org");
    }
}
