package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link ForceLoginCommand}.
 */
@ExtendWith(MockitoExtension.class)
class ForceLoginCommandTest {

    @InjectMocks
    private ForceLoginCommand command;

    @Mock
    private Management management;

    @Mock
    private PermissionsManager permissionsManager;
    
    @Mock
    private BukkitService bukkitService;

    @Test
    void shouldRejectOfflinePlayer() {
        // given
        String playerName = "Bobby";
        Player player = mockPlayer(false);
        given(bukkitService.getPlayerExact(playerName)).willReturn(player);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(playerName));

        // then
        verify(bukkitService).getPlayerExact(playerName);
        verify(sender).sendMessage("Player needs to be online!");
        verifyNoInteractions(management);
    }

    @Test
    void shouldRejectInexistentPlayer() {
        // given
        String playerName = "us3rname01";
        given(bukkitService.getPlayerExact(playerName)).willReturn(null);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(playerName));

        // then
        verify(bukkitService).getPlayerExact(playerName);
        verify(sender).sendMessage("Player needs to be online!");
        verifyNoInteractions(management);
    }

    @Test
    void shouldRejectPlayerWithMissingPermission() {
        // given
        String playerName = "testTest";
        Player player = mockPlayer(true);
        given(bukkitService.getPlayerExact(playerName)).willReturn(player);
        given(permissionsManager.hasPermission(player, PlayerPermission.CAN_LOGIN_BE_FORCED)).willReturn(false);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(playerName));

        // then
        verify(bukkitService).getPlayerExact(playerName);
        verify(sender).sendMessage(argThat(containsString("You cannot force login the player")));
        verifyNoInteractions(management);
    }

    @Test
    void shouldForceLoginPlayer() {
        // given
        String playerName = "tester23";
        Player player = mockPlayer(true);
        given(bukkitService.getPlayerExact(playerName)).willReturn(player);
        given(permissionsManager.hasPermission(player, PlayerPermission.CAN_LOGIN_BE_FORCED)).willReturn(true);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(playerName));

        // then
        verify(bukkitService).getPlayerExact(playerName);
        verify(management).forceLogin(player);
    }

    @Test
    void shouldForceLoginSenderSelf() {
        // given
        String senderName = "tester23";
        Player player = mockPlayer(true);
        given(bukkitService.getPlayerExact(senderName)).willReturn(player);
        given(permissionsManager.hasPermission(player, PlayerPermission.CAN_LOGIN_BE_FORCED)).willReturn(true);
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn(senderName);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(bukkitService).getPlayerExact(senderName);
        verify(management).forceLogin(player);
    }

    private static Player mockPlayer(boolean isOnline) {
        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(isOnline);
        return player;
    }
}
