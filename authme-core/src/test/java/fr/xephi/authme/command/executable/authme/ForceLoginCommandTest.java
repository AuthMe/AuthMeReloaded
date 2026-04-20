package fr.xephi.authme.command.executable.authme;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

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
@MockitoSettings(strictness = Strictness.WARN)
public class ForceLoginCommandTest {

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

    @Test
    public void shouldRejectOfflinePlayer() {
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
    public void shouldRejectInexistentPlayer() {
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
    public void shouldRejectPlayerWithMissingPermission() {
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
    public void shouldForceLoginPlayer() {
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
    public void shouldForceLoginSenderSelf() {
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

    private static Player mockPlayer(boolean isOnline) {
        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(isOnline);
        return player;
    }
}


