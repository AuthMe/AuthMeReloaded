package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.listener.OfflinePlayerInfo;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PermissionHandler {

    /**
     * Get the permission system that is being used.
     *
     * @return The permission system.
     */
    PermissionsSystemType getPermissionSystem();

    /**
     * Check whether the current permissions system has group support.
     * If no permissions system is hooked, false will be returned.
     *
     * @return True if the current permissions system supports groups, false otherwise.
     */
    boolean hasGroupSupport();

    /**
     * Check if a player has permission by their name.
     * Used to check an offline player's permission.
     *
     * @param offlineInfo The offline player info.
     * @param node The permission node.
     *
     * @return True if the player has permission.
     */
    CompletableFuture<Boolean> hasPermissionOffline(OfflinePlayerInfo offlineInfo, PermissionNode node);

    /**
     * Get the permission groups of a player, if available.
     *
     * @param player The player.
     *
     * @return Permission groups, or an empty list if this feature is not supported.
     */
    List<String> getGroups(Player player);

    /**
     * Get the permission groups of an offline player, if available.
     *
     * @param offlineInfo The offline player info.
     *
     * @return Permission groups, or an empty list if this feature is not supported.
     */
    CompletableFuture<List<String>> getGroupsOffline(OfflinePlayerInfo offlineInfo);

    /**
     * Get the primary group of a player, if available.
     *
     * @param player The player.
     *
     * @return The optional name of the primary permission group, empty if not found.
     */
    default Optional<String> getPrimaryGroup(Player player) {
        Collection<String> groups = getGroups(player);
        if(groups.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(groups.iterator().next());
    }

    /**
     * Get the primary group of a player, if available.
     *
     * @param offlineInfo The offline player info.
     *
     * @return The optional name of the primary permission group, empty if not found.
     */
    default CompletableFuture<Optional<String>> getPrimaryGroupOffline(OfflinePlayerInfo offlineInfo) {
        return CompletableFuture.supplyAsync(() -> {
            Collection<String> groups = getGroupsOffline(offlineInfo).join();
            if(groups.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(groups.iterator().next());
        });
    }

    /**
     * Check whether the player is in the specified group.
     *
     * @param player    The player.
     * @param groupName The group name.
     *
     * @return True if the player is in the specified group, false otherwise.
     *         False is also returned if groups aren't supported by the used permissions system.
     */
    default boolean isInGroup(Player player, String groupName) {
        return getGroups(player).contains(groupName);
    }

    /**
     * Check whether the player is in the specified group.
     *
     * @param offlineInfo    The player.
     * @param groupName The group name.
     *
     * @return True if the player is in the specified group, false otherwise.
     *         False is also returned if groups aren't supported by the used permissions system.
     */
    default CompletableFuture<Boolean> isInGroupOffline(OfflinePlayerInfo offlineInfo, String groupName) {
        return CompletableFuture.supplyAsync(() -> getGroupsOffline(offlineInfo).join().contains(groupName));
    }

    /**
     * Add the permission group of a player, if supported.
     *
     * @param player    The player
     * @param groupName The name of the group.
     *
     * @return True if succeed, false otherwise.
     *         False is also returned if this feature isn't supported for the current permissions system.
     */
    boolean addToGroup(Player player, String groupName);

    /**
     * Add the permission group of a player, if supported.
     *
     * @param offlineInfo The offline player info.
     * @param groupName The name of the group.
     *
     * @return True if succeed, false otherwise.
     *         False is also returned if this feature isn't supported for the current permissions system.
     */
    CompletableFuture<Boolean> addToGroupOffline(OfflinePlayerInfo offlineInfo, String groupName);

    /**
     * Remove the permission group of a player, if supported.
     *
     * @param player    The player
     * @param groupName The name of the group.
     *
     * @return True if succeed, false otherwise.
     *         False is also returned if this feature isn't supported for the current permissions system.
     */
    boolean removeFromGroup(Player player, String groupName);

    /**
     * Remove the permission group of a player, if supported.
     *
     * @param playerInfo The offline player info.
     * @param groupName  The name of the group.
     *
     * @return True if succeed, false otherwise.
     *         False is also returned if this feature isn't supported for the current permissions system.
     */
    CompletableFuture<Boolean> removeFromGroupOffline(OfflinePlayerInfo playerInfo, String groupName);

    /**
     * Set the permission group of a player, if supported.
     * This clears the current groups of the player.
     *
     * @param player The player.
     * @param group  The name of the group.
     *
     * @return True if succeed, false otherwise.
     *         False is also returned if this feature isn't supported for the current permissions system.
     */
    boolean setGroup(Player player, String group);

    /**
     * Set the permission group of a player, if supported.
     * This clears the current groups of the player.
     *
     * @param offlineInfo The offline player info.
     * @param group       The name of the group.
     *
     * @return True if succeed, false otherwise.
     *         False is also returned if this feature isn't supported for the current permissions system.
     */
    CompletableFuture<Boolean> setGroupOffline(OfflinePlayerInfo offlineInfo, String group);

}
