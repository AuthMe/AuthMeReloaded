package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.bukkit.entity.Player;

import java.util.List;


public interface PermissionHandler {

    /**
     * Add the permission group of a player, if supported.
     *
     * @param player    The player
     * @param group The name of the group.
     *
     * @return True if succeed, false otherwise.
     * False is also returned if this feature isn't supported for the current permissions system.
     */
    boolean addToGroup(Player player, String group);

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
     * @param name  The player's name.
     * @param node The permission node.
     *
     * @return True if the player has permission.
     */
    boolean hasPermissionOffline(String name, PermissionNode node);

    /**
     * Check whether the player is in the specified group.
     *
     * @param player    The player.
     * @param group The group name.
     *
     * @return True if the player is in the specified group, false otherwise.
     * False is also returned if groups aren't supported by the used permissions system.
     */
    boolean isInGroup(Player player, String group);

    /**
     * Remove the permission group of a player, if supported.
     *
     * @param player    The player
     * @param group The name of the group.
     *
     * @return True if succeed, false otherwise.
     * False is also returned if this feature isn't supported for the current permissions system.
     */
    boolean removeFromGroup(Player player, String group);

    /**
     * Set the permission group of a player, if supported.
     * This clears the current groups of the player.
     *
     * @param player    The player
     * @param group The name of the group.
     *
     * @return True if succeed, false otherwise.
     * False is also returned if this feature isn't supported for the current permissions system.
     */
    boolean setGroup(Player player, String group);

    /**
     * Get the permission groups of a player, if available.
     *
     * @param player The player.
     *
     * @return Permission groups, or an empty list if this feature is not supported.
     */
    List<String> getGroups(Player player);

    /**
     * Get the primary group of a player, if available.
     *
     * @param player The player.
     *
     * @return The name of the primary permission group. Or null.
     */
    String getPrimaryGroup(Player player);

    /**
     * Get the permission system that is being used.
     *
     * @return The permission system.
     */
    PermissionsSystemType getPermissionSystem();
}
