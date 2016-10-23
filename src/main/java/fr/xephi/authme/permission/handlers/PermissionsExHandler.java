package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.bukkit.entity.Player;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.ArrayList;
import java.util.List;

public class PermissionsExHandler implements PermissionHandler {

    private PermissionManager permissionManager;

    public PermissionsExHandler() throws PermissionHandlerException {
        permissionManager = PermissionsEx.getPermissionManager();
        if (permissionManager == null) {
            throw new PermissionHandlerException("Could not get manager of PermissionsEx");
        }
    }

    @Override
    public boolean addToGroup(Player player, String group) {
        if (!PermissionsEx.getPermissionManager().getGroupNames().contains(group)) {
            return false;
        }

        PermissionUser user = PermissionsEx.getUser(player);
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
    public boolean isInGroup(Player player, String group) {
        PermissionUser user = permissionManager.getUser(player);
        return user.inGroup(group);
    }

    @Override
    public boolean removeFromGroup(Player player, String group) {
        PermissionUser user = permissionManager.getUser(player);
        user.removeGroup(group);
        return true;
    }

    @Override
    public boolean setGroup(Player player, String group) {
        List<String> groups = new ArrayList<>();
        groups.add(group);

        PermissionUser user = permissionManager.getUser(player);
        user.setParentsIdentifier(groups);
        return true;
    }

    @Override
    public List<String> getGroups(Player player) {
        PermissionUser user = permissionManager.getUser(player);
        return user.getParentIdentifiers(null);
    }

    @Override
    public String getPrimaryGroup(Player player) {
        PermissionUser user = permissionManager.getUser(player);

        List<String> groups = user.getParentIdentifiers(null);
        if (groups.size() == 0)
            return null;

        return groups.get(0);
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.PERMISSIONS_EX;
    }
}
