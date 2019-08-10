package fr.xephi.authme.permission;

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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
}
