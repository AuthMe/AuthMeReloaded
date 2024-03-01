package fr.xephi.authme.permission;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PermissionsManager}.
 */
@ExtendWith(MockitoExtension.class)
class PermissionsManagerTest {

    @InjectMocks
    private PermissionsManager permissionsManager;

    @Mock
    private Server server;

    @Mock
    private PluginManager pluginManager;

    @Test
    void shouldUseDefaultPermissionForCommandSender() {
        // given
        PermissionNode node = TestPermissions.LOGIN;
        CommandSender sender = mock(CommandSender.class);

        // when
        boolean result = permissionsManager.hasPermission(sender, node);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    void shouldGrantToOpCommandSender() {
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
    void shouldDenyPermissionEvenForOpCommandSender() {
        // given
        PermissionNode node = TestPermissions.WORLD_DOMINATION;
        CommandSender sender = mock(CommandSender.class);

        // when
        boolean result = permissionsManager.hasPermission(sender, node);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    void shouldAllowForNonOpPlayer() {
        // given
        PermissionNode node = TestPermissions.LOGIN;
        Player player = mock(Player.class);

        // when
        boolean result = permissionsManager.hasPermission(player, node);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    void shouldDenyForNonOpPlayer() {
        // given
        PermissionNode node = TestPermissions.DELETE_USER;
        Player player = mock(Player.class);

        // when
        boolean result = permissionsManager.hasPermission(player, node);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    void shouldAllowForOpPlayer() {
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
    void shouldDenyEvenForOpPlayer() {
        // given
        PermissionNode node = TestPermissions.WORLD_DOMINATION;
        Player player = mock(Player.class);

        // when
        boolean result = permissionsManager.hasPermission(player, node);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    void shouldHandleNullPermissionForCommandSender() {
        // given
        PermissionNode node = null;
        CommandSender sender = mock(CommandSender.class);

        // when
        boolean result = permissionsManager.hasPermission(sender, node);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    void shouldHandleNullPermissionForPlayer() {
        // given
        PermissionNode node = null;
        Player player = mock(Player.class);

        // when
        boolean result = permissionsManager.hasPermission(player, node);

        // then
        assertThat(result, equalTo(true));
    }
}
