package fr.xephi.authme.permission.handlers;

import de.bananaco.bpermissions.api.ApiLayer;
import de.bananaco.bpermissions.api.CalculableType;
import fr.xephi.authme.OfflinePlayerWrapper;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Handler for bPermissions.
 *
 * @see <a href="https://dev.bukkit.org/projects/bpermissions">bPermissions Bukkit page</a>
 * @see <a href="https://github.com/rymate1234/bPermissions/">bPermissions on Github</a>
 */
public class BPermissionsHandler implements PermissionHandler {

    @Override
    public CompletableFuture<Boolean> addToGroup(OfflinePlayerWrapper player, String groupName) {
        ApiLayer.addGroup(null, CalculableType.USER, player.getName(), groupName);
        return completedFuture(true);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> hasPermissionOffline(OfflinePlayerWrapper player, PermissionNode node) {
        return completedFuture(ApiLayer.hasPermission(null, CalculableType.USER, player.getName(), node.getNode()));
    }

    @Override
    public CompletableFuture<Boolean> isInGroup(OfflinePlayerWrapper player, String groupName) {
        return completedFuture(ApiLayer.hasGroup(null, CalculableType.USER, player.getName(), groupName));
    }

    @Override
    public CompletableFuture<Boolean> removeFromGroup(OfflinePlayerWrapper player, String groupName) {
        ApiLayer.removeGroup(null, CalculableType.USER, player.getName(), groupName);
        return completedFuture(true);
    }

    @Override
    public CompletableFuture setGroup(OfflinePlayerWrapper player, String group) {
        ApiLayer.setGroup(null, CalculableType.USER, player.getName(), group);
        return completedFuture(true);
    }

    @Override
    public CompletableFuture<List<String>> getGroups(OfflinePlayerWrapper player) {
        return completedFuture(Arrays.asList(ApiLayer.getGroups(null, CalculableType.USER, player.getName())));
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.B_PERMISSIONS;
    }
}
