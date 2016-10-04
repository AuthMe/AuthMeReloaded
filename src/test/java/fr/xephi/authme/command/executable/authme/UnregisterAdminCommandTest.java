package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link UnregisterAdminCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class UnregisterAdminCommandTest {

    @InjectMocks
    private UnregisterAdminCommand command;

    @Mock
    private DataSource dataSource;

    @Mock
    private CommandService commandService;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private Management management;

    @Test
    public void shouldHandleUnknownPlayer() {
        // given
        String user = "bobby";
        given(dataSource.isAuthAvailable(user)).willReturn(false);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(user));

        // then
        verify(dataSource, only()).isAuthAvailable(user);
        verify(commandService).send(sender, MessageKey.UNKNOWN_USER);
    }

    @Test
    public void shouldInvokeUnregisterProcess() {
        // given
        String user = "personaNonGrata";
        given(dataSource.isAuthAvailable(user)).willReturn(true);
        given(dataSource.removeAuth(user)).willReturn(false);
        Player player = mock(Player.class);
        given(bukkitService.getPlayerExact(user)).willReturn(player);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(user));

        // then
        verify(dataSource, only()).isAuthAvailable(user);
        verify(bukkitService).getPlayerExact(user);
        verify(management).performUnregisterByAdmin(sender, user, player);
    }

    @Test
    public void shouldInvokeUnregisterProcessWithNullPlayer() {
        // given
        String user = "personaNonGrata";
        given(dataSource.isAuthAvailable(user)).willReturn(true);
        given(dataSource.removeAuth(user)).willReturn(false);
        given(bukkitService.getPlayerExact(user)).willReturn(null);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(user));

        // then
        verify(dataSource, only()).isAuthAvailable(user);
        verify(bukkitService).getPlayerExact(user);
        verify(management).performUnregisterByAdmin(sender, user, null);
    }

}
