package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.OfflinePlayerWrapper;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.bukkit.OfflinePlayer;

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
     * @param player  The offline player.
     * @param node The permission node.
     *
     * @return True if the player has permission.
     */
    CompletableFuture<Boolean> hasPermissionOffline(OfflinePlayerWrapper player, PermissionNode node);

    /**
     * Add the permission group of a player, if supported.
     *
     * @param player    The player
     * @param groupName The name of the group.
     *
     * @return True if succeed, false otherwise.
     *         False is also returned if this feature isn't supported for the current permissions system.
     */
    CompletableFuture<Boolean> addToGroup(OfflinePlayerWrapper player, String groupName);

    /**
     * Check whether the player is in the specified group.
     *
     * @param player    The player.
     * @param groupName The group name.
     *
     * @return True if the player is in the specified group, false otherwise.
     *         False is also returned if groups aren't supported by the used permissions system.
     */
    default CompletableFuture<Boolean> isInGroup(OfflinePlayerWrapper player, String groupName) {
        return CompletableFuture.supplyAsync(() -> getGroups(player).join().contains(groupName));
    }

    /**
     * Remove the permission group of a player, if supported.
     *
     * @param player    The player
     * @param groupName The name of the group.
     *
     * @return True if succeed, false otherwise.
     *         False is also returned if this feature isn't supported for the current permissions system.
     */
    CompletableFuture<Boolean> removeFromGroup(OfflinePlayerWrapper player, String groupName);

    /**
     * Set the permission group of a player, if supported.
     * This clears the current groups of the player.
     *
     * @param player    The player
     * @param group The name of the group.
     *
     * @return True if succeed, false otherwise.
     *         False is also returned if this feature isn't supported for the current permissions system.
     */
    CompletableFuture<Boolean> setGroup(OfflinePlayerWrapper player, String group);

    /**
     * Get the permission groups of a player, if available.
     *
     * @param player The player.
     *
     * @return Permission groups, or an empty list if this feature is not supported.
     */
    CompletableFuture<List<String>> getGroups(OfflinePlayerWrapper player);

    /**
     * Get the primary group of a player, if available.
     *
     * @param player The player.
     *
     * @return The name of the primary permission group. Or null.
     */
    default CompletableFuture<Optional<String>> getPrimaryGroup(OfflinePlayerWrapper player) {
        return CompletableFuture.supplyAsync(() -> {
            Collection<String> groups = getGroups(player).join();
            if(groups.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(groups.iterator().next());
        });
    }

}
