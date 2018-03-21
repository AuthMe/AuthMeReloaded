package fr.xephi.authme.permission.handlers;

import de.bananaco.bpermissions.api.ApiLayer;
import de.bananaco.bpermissions.api.CalculableType;
import fr.xephi.authme.OfflinePlayerInfo;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.bukkit.entity.Player;

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
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.B_PERMISSIONS;
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> hasPermissionOffline(OfflinePlayerInfo offlineInfo, PermissionNode node) {
        return completedFuture(ApiLayer.hasPermission(null, CalculableType.USER, offlineInfo.getName(), node.getNode()));
    }

    @Override
    public List<String> getGroups(Player player) {
        return Arrays.asList(ApiLayer.getGroups(null, CalculableType.USER, player.getName()));
    }

    @Override
    public CompletableFuture<List<String>> getGroupsOffline(OfflinePlayerInfo offlineInfo) {
        return completedFuture(Arrays.asList(ApiLayer.getGroups(null, CalculableType.USER, offlineInfo.getName())));
    }

    @Override
    public boolean isInGroup(Player player, String groupName) {
        return ApiLayer.hasGroup(null, CalculableType.USER, player.getName(), groupName);
    }

    @Override
    public CompletableFuture<Boolean> isInGroupOffline(OfflinePlayerInfo offlineInfo, String groupName) {
        return completedFuture(ApiLayer.hasGroup(null, CalculableType.USER, offlineInfo.getName(), groupName));
    }

    @Override
    public boolean addToGroup(Player player, String groupName) {
        ApiLayer.addGroup(null, CalculableType.USER, player.getName(), groupName);
        return true;
    }

    @Override
    public CompletableFuture<Boolean> addToGroupOffline(OfflinePlayerInfo offlineInfo, String groupName) {
        ApiLayer.addGroup(null, CalculableType.USER, offlineInfo.getName(), groupName);
        return completedFuture(true);
    }

    @Override
    public boolean removeFromGroup(Player player, String groupName) {
        ApiLayer.removeGroup(null, CalculableType.USER, player.getName(), groupName);
        return true;
    }

    @Override
    public CompletableFuture<Boolean> removeFromGroupOffline(OfflinePlayerInfo playerInfo, String groupName) {
        ApiLayer.removeGroup(null, CalculableType.USER, playerInfo.getName(), groupName);
        return completedFuture(true);
    }

    @Override
    public boolean setGroup(Player player, String group) {
        ApiLayer.setGroup(null, CalculableType.USER, player.getName(), group);
        return true;
    }

    @Override
    public CompletableFuture<Boolean> setGroupOffline(OfflinePlayerInfo offlineInfo, String group) {
        ApiLayer.setGroup(null, CalculableType.USER, offlineInfo.getName(), group);
        return completedFuture(true);
    }

}
