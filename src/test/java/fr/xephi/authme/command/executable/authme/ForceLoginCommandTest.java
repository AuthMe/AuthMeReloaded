package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.process.Management;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link ForceLoginCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ForceLoginCommandTest {

    @Mock
    private CommandService commandService;

    @Test
    public void shouldRejectOfflinePlayer() {
        // given
        String playerName = "Bobby";
        Player player = mockPlayer(false, playerName);
        given(commandService.getPlayer(playerName)).willReturn(player);
        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand command = new ForceLoginCommand();

        // when
        command.executeCommand(sender, Collections.singletonList(playerName), commandService);

        // then
        verify(commandService).getPlayer(playerName);
        verify(sender).sendMessage(argThat(equalTo("Player needs to be online!")));
        verify(commandService, never()).getManagement();
    }

    @Test
    public void shouldRejectInexistentPlayer() {
        // given
        String playerName = "us3rname01";
        given(commandService.getPlayer(playerName)).willReturn(null);
        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand command = new ForceLoginCommand();

        // when
        command.executeCommand(sender, Collections.singletonList(playerName), commandService);

        // then
        verify(commandService).getPlayer(playerName);
        verify(sender).sendMessage(argThat(equalTo("Player needs to be online!")));
        verify(commandService, never()).getManagement();
    }

    @Test
    public void shouldRejectPlayerWithMissingPermission() {
        // given
        String playerName = "testTest";
        Player player = mockPlayer(true, playerName);
        given(commandService.getPlayer(playerName)).willReturn(player);
        PermissionsManager permissionsManager = mock(PermissionsManager.class);
        given(permissionsManager.hasPermission(player, PlayerPermission.CAN_LOGIN_BE_FORCED)).willReturn(false);
        given(commandService.getPermissionsManager()).willReturn(permissionsManager);

        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand command = new ForceLoginCommand();

        // when
        command.executeCommand(sender, Collections.singletonList(playerName), commandService);

        // then
        verify(commandService).getPlayer(playerName);
        verify(sender).sendMessage(argThat(containsString("You cannot force login the player")));
        verify(commandService, never()).getManagement();
    }

    @Test
    public void shouldForceLoginPlayer() {
        // given
        String playerName = "tester23";
        Player player = mockPlayer(true, playerName);
        given(commandService.getPlayer(playerName)).willReturn(player);
        PermissionsManager permissionsManager = mock(PermissionsManager.class);
        given(permissionsManager.hasPermission(player, PlayerPermission.CAN_LOGIN_BE_FORCED)).willReturn(true);
        given(commandService.getPermissionsManager()).willReturn(permissionsManager);
        Management management = mock(Management.class);
        given(commandService.getManagement()).willReturn(management);

        CommandSender sender = mock(CommandSender.class);
        ExecutableCommand command = new ForceLoginCommand();

        // when
        command.executeCommand(sender, Collections.singletonList(playerName), commandService);

        // then
        verify(commandService).getPlayer(playerName);
        verify(management).performLogin(eq(player), anyString(), eq(true));
    }

    @Test
    public void shouldForceLoginSenderSelf() {
        // given
        String senderName = "tester23";
        Player player = mockPlayer(true, senderName);
        given(commandService.getPlayer(senderName)).willReturn(player);
        PermissionsManager permissionsManager = mock(PermissionsManager.class);
        given(permissionsManager.hasPermission(player, PlayerPermission.CAN_LOGIN_BE_FORCED)).willReturn(true);
        given(commandService.getPermissionsManager()).willReturn(permissionsManager);
        Management management = mock(Management.class);
        given(commandService.getManagement()).willReturn(management);

        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn(senderName);
        ExecutableCommand command = new ForceLoginCommand();

        // when
        command.executeCommand(sender, Collections.<String>emptyList(), commandService);

        // then
        verify(commandService).getPlayer(senderName);
        verify(management).performLogin(eq(player), anyString(), eq(true));
    }

    private static Player mockPlayer(boolean isOnline, String name) {
        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(isOnline);
        given(player.getName()).willReturn(name);
        return player;
    }
}
