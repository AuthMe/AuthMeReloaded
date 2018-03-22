package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.listener.OfflinePlayerInfo;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static fr.xephi.authme.util.OptionalUtils.handleOptional;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Handler for PermissionsEx.
 *
 * @see <a href="https://dev.bukkit.org/projects/permissionsex">PermissionsEx Bukkit page</a>
 * @see <a href="https://github.com/PEXPlugins/PermissionsEx">PermissionsEx on Github</a>
 */
public class PermissionsExHandler implements SimplePermissionHandler {

    private PermissionManager permissionManager;

    public PermissionsExHandler() throws PermissionHandlerException {
        permissionManager = PermissionsEx.getPermissionManager();
        if (permissionManager == null) {
            throw new PermissionHandlerException("Could not get manager of PermissionsEx");
        }
    }

    private boolean isGroupValid(String groupName) {
        return permissionManager.getGroupNames().contains(groupName);
    }

    private PermissionUser getUserOffline(OfflinePlayerInfo player) {
        return handleOptional(player.getUniqueId(), uuid -> permissionManager.getUser(uuid), permissionManager.getUser(player.getName()));
    }

    @Override
    public CompletableFuture<Boolean> hasPermissionOffline(OfflinePlayerInfo offlinePlayerInfo, PermissionNode node) {
        return completedFuture(getUserOffline(offlinePlayerInfo).has(node.getNode()));
    }

    @Override
    public CompletableFuture<Boolean> addToGroupOffline(OfflinePlayerInfo offlineInfo, String groupName) {
        if (!isGroupValid(groupName)) {
            return completedFuture(false);
        }
        getUserOffline(offlineInfo).addGroup(groupName);
        return completedFuture(true);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> isInGroupOffline(OfflinePlayerInfo offlineInfo, String groupName) {
        return completedFuture(getUserOffline(offlineInfo).inGroup(groupName));
    }

    @Override
    public CompletableFuture<Boolean> removeFromGroupOffline(OfflinePlayerInfo offlineInfo, String groupName) {
        if (!isGroupValid(groupName)) {
            return completedFuture(false);
        }
        getUserOffline(offlineInfo).removeGroup(groupName);
        return completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> setGroupOffline(OfflinePlayerInfo offlineInfo, String groupName) {
        if (!isGroupValid(groupName)) {
            return completedFuture(false);
        }
        List<String> groups = new ArrayList<>();
        groups.add(groupName);
        getUserOffline(offlineInfo).setParentsIdentifier(groups);
        return completedFuture(true);
    }

    @Override
    public CompletableFuture<List<String>> getGroupsOffline(OfflinePlayerInfo offlineInfo) {
        return completedFuture(getUserOffline(offlineInfo).getParentIdentifiers(null));
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.PERMISSIONS_EX;
    }

}
