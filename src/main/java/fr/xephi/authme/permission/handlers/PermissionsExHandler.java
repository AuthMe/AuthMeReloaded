package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.OfflinePlayerWrapper;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.bukkit.OfflinePlayer;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Handler for PermissionsEx.
 *
 * @see <a href="https://dev.bukkit.org/projects/permissionsex">PermissionsEx Bukkit page</a>
 * @see <a href="https://github.com/PEXPlugins/PermissionsEx">PermissionsEx on Github</a>
 */
public class PermissionsExHandler implements PermissionHandler {

    private PermissionManager permissionManager;

    public PermissionsExHandler() throws PermissionHandlerException {
        permissionManager = PermissionsEx.getPermissionManager();
        if (permissionManager == null) {
            throw new PermissionHandlerException("Could not get manager of PermissionsEx");
        }
    }

    private PermissionUser getUser(OfflinePlayerWrapper player) {
        try {
            return permissionManager.getUser(player.getUniqueId());
        } catch (NoSuchMethodError e) {
            return permissionManager.getUser(player.getName());
        }
    }

    @Override
    public CompletableFuture<Boolean> addToGroup(OfflinePlayerWrapper player, String groupName) {
        if (!permissionManager.getGroupNames().contains(groupName)) {
            return completedFuture(false);
        }
        permissionManager.getUser(player.getName()).addGroup(groupName);
        return completedFuture(true);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> hasPermissionOffline(OfflinePlayerWrapper player, PermissionNode node) {
        return completedFuture(getUser(player).has(node.getNode()));
    }

    @Override
    public CompletableFuture<Boolean> isInGroup(OfflinePlayerWrapper player, String groupName) {
        return completedFuture(getUser(player).inGroup(groupName));
    }

    @Override
    public CompletableFuture<Boolean> removeFromGroup(OfflinePlayerWrapper player, String groupName) {
        getUser(player).removeGroup(groupName);
        return completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> setGroup(OfflinePlayerWrapper player, String group) {
        List<String> groups = new ArrayList<>();
        groups.add(group);

        getUser(player).setParentsIdentifier(groups);
        return completedFuture(true);
    }

    @Override
    public CompletableFuture<List<String>> getGroups(OfflinePlayerWrapper player) {
        return completedFuture(getUser(player).getParentIdentifiers(null));
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.PERMISSIONS_EX;
    }
}
