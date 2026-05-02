package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.PreJoinDialogService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Mock
    private Messages messages;

    @Mock
    private PreJoinDialogService preJoinDialogService;

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
        verify(messages).send(sender, MessageKey.FORCE_LOGIN_PLAYER_OFFLINE);
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
        verify(messages).send(sender, MessageKey.FORCE_LOGIN_PLAYER_OFFLINE);
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
        verify(messages).send(eq(sender), eq(MessageKey.FORCE_LOGIN_FORBIDDEN), eq(playerName));
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
        verify(messages).send(eq(sender), eq(MessageKey.FORCE_LOGIN_SUCCESS), eq(playerName));
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
        verify(messages).send(eq(sender), eq(MessageKey.FORCE_LOGIN_SUCCESS), eq(senderName));
    }

    @Test
    void shouldForceLoginPlayerBlockedInPreJoinDialog() {
        // given
        String playerName = "Connor";
        given(bukkitService.getPlayerExact(playerName)).willReturn(null);
        given(preJoinDialogService.approvePreJoinForceLogin("connor")).willReturn(true);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(playerName));

        // then
        verify(preJoinDialogService).approvePreJoinForceLogin("connor");
        verify(messages).send(eq(sender), eq(MessageKey.FORCE_LOGIN_SUCCESS), eq(playerName));
        verifyNoInteractions(management);
    }

    @Test
    void shouldSendOfflineMessageWhenPlayerNotFoundAndNoPreJoinDialog() {
        // given
        String playerName = "NotConnecting";
        given(bukkitService.getPlayerExact(playerName)).willReturn(null);
        given(preJoinDialogService.approvePreJoinForceLogin("notconnecting")).willReturn(false);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(playerName));

        // then
        verify(preJoinDialogService).approvePreJoinForceLogin("notconnecting");
        verify(messages).send(sender, MessageKey.FORCE_LOGIN_PLAYER_OFFLINE);
        verifyNoInteractions(management);
    }

    private static Player mockPlayer(boolean isOnline) {
        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(isOnline);
        return player;
    }
}
