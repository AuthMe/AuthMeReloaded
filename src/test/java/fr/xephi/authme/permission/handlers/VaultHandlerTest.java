package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.data.limbo.UserGroup;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link VaultHandler}.
 */
public class VaultHandlerTest {

    private VaultHandlerTestImpl vaultHandlerTest = VaultHandlerTestImpl.create();

    @Test
    public void shouldReturnGroups() {
        // given
        Permission permissionMock = vaultHandlerTest.permissionMock;
        Player player = mock(Player.class);
        given(permissionMock.getPlayerGroups(null, player)).willReturn(new String[]{"abc", "test"});

        // when
        List<UserGroup> result = vaultHandlerTest.getGroups(player);

        // then
        List<String> groupNames = result.stream().map(UserGroup::getGroupName).collect(toList());
        assertThat(groupNames, contains("abc", "test"));
        verify(permissionMock).getPlayerGroups(null, player);
    }

    /**
     * Bug #1702: VaultHandler may return null for groups list.
     */
    @Test
    public void shouldHandleNullAsGroups() {
        // given
        Permission permissionMock = vaultHandlerTest.permissionMock;
        Player player = mock(Player.class);
        given(permissionMock.getPlayerGroups(null, player)).willReturn(null);

        // when
        List<UserGroup> result = vaultHandlerTest.getGroups(player);

        // then
        assertThat(result, empty());
        verify(permissionMock).getPlayerGroups(null, player);
    }

    /** Test implementation using a mock Vault Permission instance. */
    private static final class VaultHandlerTestImpl extends VaultHandler {

        private Permission permissionMock;

        VaultHandlerTestImpl() throws PermissionHandlerException {
            super(null);
        }

        static VaultHandlerTestImpl create() {
            try {
                return new VaultHandlerTestImpl();
            } catch (PermissionHandlerException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        Permission getVaultPermission(Server server) {
            permissionMock = mock(Permission.class);
            return permissionMock;
        }
    }
}
