package fr.xephi.authme.permission;

import fr.xephi.authme.listener.JoiningPlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link PermissionsManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PermissionsManagerTest {

    @InjectMocks
    private PermissionsManager permissionsManager;

    @Mock
    private Server server;

    @Mock
    private PluginManager pluginManager;

    @Test
    public void shouldUseDefaultPermissionForCommandSender() {
        // given
        PermissionNode node = TestPermissions.LOGIN;
        CommandSender sender = mock(CommandSender.class);

        // when
        boolean result = permissionsManager.hasPermission(sender, node);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldGrantToOpCommandSender() {
        // given
        PermissionNode node = TestPermissions.DELETE_USER;
        CommandSender sender = mock(CommandSender.class);
        given(sender.isOp()).willReturn(true);

        // when
        boolean result = permissionsManager.hasPermission(sender, node);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldDenyPermissionEvenForOpCommandSender() {
        // given
        PermissionNode node = TestPermissions.WORLD_DOMINATION;
        CommandSender sender = mock(CommandSender.class);

        // when
        boolean result = permissionsManager.hasPermission(sender, node);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldAllowForNonOpPlayer() {
        // given
        PermissionNode node = TestPermissions.LOGIN;
        Player player = mock(Player.class);

        // when
        boolean result = permissionsManager.hasPermission(player, node);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldDenyForNonOpPlayer() {
        // given
        PermissionNode node = TestPermissions.DELETE_USER;
        Player player = mock(Player.class);

        // when
        boolean result = permissionsManager.hasPermission(player, node);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldAllowForOpPlayer() {
        // given
        PermissionNode node = TestPermissions.DELETE_USER;
        Player player = mock(Player.class);
        given(player.isOp()).willReturn(true);

        // when
        boolean result = permissionsManager.hasPermission(player, node);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldDenyEvenForOpPlayer() {
        // given
        PermissionNode node = TestPermissions.WORLD_DOMINATION;
        Player player = mock(Player.class);

        // when
        boolean result = permissionsManager.hasPermission(player, node);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldHandleNullPermissionForCommandSender() {
        // given
        PermissionNode node = null;
        CommandSender sender = mock(CommandSender.class);

        // when
        boolean result = permissionsManager.hasPermission(sender, node);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldHandleNullPermissionForPlayer() {
        // given
        PermissionNode node = null;
        Player player = mock(Player.class);

        // when
        boolean result = permissionsManager.hasPermission(player, node);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldHandleJoiningPlayerPermissionChecksWithProperMethod() {
        // given
        Player player = mock(Player.class);
        JoiningPlayer fromPlayer = JoiningPlayer.fromPlayerObject(player);
        JoiningPlayer fromName = JoiningPlayer.fromName("Chris");

        PermissionsManager permManagerSpy = spy(permissionsManager);
        given(permManagerSpy.hasPermission(any(Player.class), eq(PlayerPermission.LOGIN))).willReturn(true);
        given(permManagerSpy.hasPermissionOffline(anyString(), eq(PlayerPermission.UNREGISTER))).willReturn(true);

        // when
        boolean resultFromPlayer = permManagerSpy.hasPermission(fromPlayer, PlayerPermission.LOGIN);
        boolean resultFromName = permManagerSpy.hasPermission(fromName, PlayerPermission.UNREGISTER);

        // then
        assertThat(resultFromPlayer, equalTo(true));
        assertThat(resultFromName, equalTo(true));
        verify(permManagerSpy).hasPermission(player, PlayerPermission.LOGIN);
        verify(permManagerSpy).hasPermissionOffline(fromName.getName(), PlayerPermission.UNREGISTER);
    }
}
