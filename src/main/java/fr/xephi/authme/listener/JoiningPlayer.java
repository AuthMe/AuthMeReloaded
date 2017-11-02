package fr.xephi.authme.listener;

import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;

/**
 * Represents a player joining the server, which depending on the available
 * information may be his name or the actual Player object.
 */
public final class JoiningPlayer {

    private final String name;
    private final BiFunction<PermissionsManager, PermissionNode, Boolean> permissionLookupFunction;

    /**
     * Hidden constructor.
     *
     * @param name the player's name
     * @param permFunction the function to use for permission lookups
     */
    private JoiningPlayer(String name, BiFunction<PermissionsManager, PermissionNode, Boolean> permFunction) {
        this.name = name;
        this.permissionLookupFunction = permFunction;
    }

    /**
     * Creates a {@link JoiningPlayer} instance from the given name.
     *
     * @param name the player's name
     * @return the created instance
     */
    public static JoiningPlayer fromName(String name) {
        return new JoiningPlayer(name, (manager, perm) -> manager.hasPermissionOffline(name, perm));
    }

    /**
     * Creates a {@link JoiningPlayer} instance from the given Player object.
     *
     * @param player the player
     * @return the created instance
     */
    public static JoiningPlayer fromPlayerObject(Player player) {
        return new JoiningPlayer(player.getName(), (manager, perm) -> manager.hasPermission(player, perm));
    }

    /**
     * @return the player's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the function to use for permission lookups. Takes two arguments: the PermissionsManager instance,
     * and the permission node to look up. The result is a boolean indicating whether or not this joining player
     * has permission.
     *
     * @return the permissions lookup function to use
     */
    public BiFunction<PermissionsManager, PermissionNode, Boolean> getPermissionLookupFunction() {
        return permissionLookupFunction;
    }
}
