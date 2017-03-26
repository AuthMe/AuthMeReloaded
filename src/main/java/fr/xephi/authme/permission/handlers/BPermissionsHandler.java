package fr.xephi.authme.permission.handlers;

import de.bananaco.bpermissions.api.ApiLayer;
import de.bananaco.bpermissions.api.CalculableType;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Handler for bPermissions.
 *
 * @see <a href="https://dev.bukkit.org/projects/bpermissions">bPermissions Bukkit page</a>
 * @see <a href="https://github.com/rymate1234/bPermissions/">bPermissions on Github</a>
 */
public class BPermissionsHandler implements PermissionHandler {

    @Override
    public boolean addToGroup(Player player, String group) {
        ApiLayer.addGroup(player.getWorld().getName(), CalculableType.USER, player.getName(), group);
        return true;
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public boolean hasPermissionOffline(String name, PermissionNode node) {
        return ApiLayer.hasPermission(null, CalculableType.USER, name, node.getNode());
    }

    @Override
    public boolean isInGroup(Player player, String group) {
        return ApiLayer.hasGroup(player.getWorld().getName(), CalculableType.USER, player.getName(), group);
    }

    @Override
    public boolean removeFromGroup(Player player, String group) {
        ApiLayer.removeGroup(player.getWorld().getName(), CalculableType.USER, player.getName(), group);
        return true;
    }

    @Override
    public boolean setGroup(Player player, String group) {
        ApiLayer.setGroup(player.getWorld().getName(), CalculableType.USER, player.getName(), group);
        return true;
    }

    @Override
    public List<String> getGroups(Player player) {
        return Arrays.asList(ApiLayer.getGroups(player.getWorld().getName(), CalculableType.USER, player.getName()));
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.B_PERMISSIONS;
    }
}
