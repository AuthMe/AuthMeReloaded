package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.data.limbo.UserGroup;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.bukkit.OfflinePlayer;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

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

    @Override
    public boolean addToGroup(OfflinePlayer player, UserGroup group) {
        if (!PermissionsEx.getPermissionManager().getGroupNames().contains(group)) {
            return false;
        }

        PermissionUser user = PermissionsEx.getUser(player.getName());
        user.addGroup(group.getGroupName());
        return true;
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public boolean hasPermissionOffline(String name, PermissionNode node) {
        PermissionUser user = permissionManager.getUser(name);
        return user.has(node.getNode());
    }

    @Override
    public boolean isInGroup(OfflinePlayer player, UserGroup group) {
        PermissionUser user = permissionManager.getUser(player.getName());
        return user.inGroup(group.getGroupName());
    }

    @Override
    public boolean removeFromGroup(OfflinePlayer player, UserGroup group) {
        PermissionUser user = permissionManager.getUser(player.getName());
        user.removeGroup(group.getGroupName());
        return true;
    }

    @Override
    public boolean setGroup(OfflinePlayer player, UserGroup group) {
        List<String> groups = new ArrayList<>();
        groups.add(group.getGroupName());

        PermissionUser user = permissionManager.getUser(player.getName());
        user.setParentsIdentifier(groups);
        return true;
    }

    @Override
    public List<UserGroup> getGroups(OfflinePlayer player) {
        PermissionUser user = permissionManager.getUser(player.getName());
        return user.getParentIdentifiers(null).stream()
            .map(i -> new UserGroup(i, null))
            .collect(toList());
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.PERMISSIONS_EX;
    }
}
