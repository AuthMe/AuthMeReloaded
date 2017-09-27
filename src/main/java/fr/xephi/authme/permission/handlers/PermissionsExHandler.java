package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.bukkit.OfflinePlayer;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.ArrayList;
import java.util.List;

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
    public boolean addToGroup(OfflinePlayer player, String group) {
        if (!PermissionsEx.getPermissionManager().getGroupNames().contains(group)) {
            return false;
        }

        PermissionUser user = PermissionsEx.getUser(player.getName());
        user.addGroup(group);
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
    public boolean isInGroup(OfflinePlayer player, String group) {
        PermissionUser user = permissionManager.getUser(player.getName());
        return user.inGroup(group);
    }

    @Override
    public boolean removeFromGroup(OfflinePlayer player, String group) {
        PermissionUser user = permissionManager.getUser(player.getName());
        user.removeGroup(group);
        return true;
    }

    @Override
    public boolean setGroup(OfflinePlayer player, String group) {
        List<String> groups = new ArrayList<>();
        groups.add(group);

        PermissionUser user = permissionManager.getUser(player.getName());
        user.setParentsIdentifier(groups);
        return true;
    }

    @Override
    public List<String> getGroups(OfflinePlayer player) {
        PermissionUser user = permissionManager.getUser(player.getName());
        return user.getParentIdentifiers(null);
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.PERMISSIONS_EX;
    }
}
