package fr.xephi.authme.listener;

import fr.xephi.authme.OfflinePlayerInfo;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Represents a player joining the server, which depending on the available
 * information may be his name or the actual Player object.
 */
public final class JoiningPlayer {

    private final String name;
    private final BiFunction<PermissionsManager, PermissionNode, CompletableFuture<Boolean>> permissionLookupFunction;

    /**
     * Hidden constructor.
     *
     * @param name the player's name
     * @param permFunction the function to use for permission lookups
     */
    private JoiningPlayer(String name, BiFunction<PermissionsManager, PermissionNode, CompletableFuture<Boolean>> permFunction) {
        this.name = name;
        this.permissionLookupFunction = permFunction;
    }

    /**
     * Creates a {@link JoiningPlayer} instance from the given name.
     *
     * @param offline the offline player information
     * @return the created instance
     */
    public static JoiningPlayer fromOfflinePlayerInfo(OfflinePlayerInfo offline) {
        return new JoiningPlayer(offline.getName(), (manager, perm) ->
            manager.hasPermissionOffline(offline, perm));
    }

    /**
     * Creates a {@link JoiningPlayer} instance from the given Player object.
     *
     * @param player the player
     * @return the created instance
     */
    public static JoiningPlayer fromPlayerObject(Player player) {
        return new JoiningPlayer(player.getName(), (manager, perm) ->
            completedFuture(manager.hasPermission(player, perm)));
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
    public BiFunction<PermissionsManager, PermissionNode, CompletableFuture<Boolean>> getPermissionLookupFunction() {
        return permissionLookupFunction;
    }
}
