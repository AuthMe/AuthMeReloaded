package fr.xephi.authme.data.auth;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used to manage player's Authenticated status
 */
public class PlayerCache {

    private final Map<String, PlayerAuth> cache = new ConcurrentHashMap<>();
    private final Map<Player, PlayerAuth> playerCache = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<String, Player> owners = new ConcurrentHashMap<>();

    PlayerCache() {
    }

    /**
     * Adds the given auth object to the player cache (for the name defined in the PlayerAuth).
     *
     * @param auth the player auth object to save
     */
    public synchronized void updatePlayer(PlayerAuth auth) {
        String normalizedName = auth.getNickname().toLowerCase(Locale.ROOT);
        cache.put(normalizedName, auth);
        synchronized (playerCache) {
            playerCache.replaceAll((player, current) -> player.getName().equalsIgnoreCase(normalizedName) ? auth : current);
        }
    }

    public synchronized void updatePlayer(Player player, PlayerAuth auth) {
        String normalizedName = auth.getNickname().toLowerCase(Locale.ROOT);
        cache.put(normalizedName, auth);
        owners.put(normalizedName, player);
        playerCache.put(player, auth);
    }

    /**
     * Removes a player from the player cache.
     *
     * @param user name of the player to remove
     */
    public synchronized void removePlayer(String user) {
        String normalizedName = user.toLowerCase(Locale.ROOT);
        cache.remove(normalizedName);
        owners.remove(normalizedName);
        synchronized (playerCache) {
            playerCache.keySet().removeIf(player -> player.getName().equalsIgnoreCase(normalizedName));
        }
    }

    public synchronized void removePlayer(Player player) {
        PlayerAuth removed = playerCache.remove(player);
        if (removed != null) {
            String normalizedName = removed.getNickname().toLowerCase(Locale.ROOT);
            if (owners.get(normalizedName) == player) {
                owners.remove(normalizedName);
                cache.remove(normalizedName);
            }
        }
    }

    /**
     * Get whether a player is authenticated (i.e. whether he is present in the player cache).
     *
     * @param user player's name
     *
     * @return true if player is logged in, false otherwise.
     */
    public boolean isAuthenticated(String user) {
        return cache.containsKey(user.toLowerCase(Locale.ROOT));
    }

    public synchronized boolean isAuthenticated(Player player) {
        PlayerAuth auth = playerCache.get(player);
        return auth != null && owners.get(auth.getNickname().toLowerCase(Locale.ROOT)) == player;
    }

    /**
     * Returns the PlayerAuth associated with the given user, if available.
     *
     * @param user name of the player
     *
     * @return the associated auth object, or null if not available
     */
    public PlayerAuth getAuth(String user) {
        return cache.get(user.toLowerCase(Locale.ROOT));
    }

    public synchronized PlayerAuth getAuth(Player player) {
        return isAuthenticated(player) ? playerCache.get(player) : null;
    }

    /**
     * @return number of logged in players
     */
    public int getLogged() {
        return cache.size();
    }

    /**
     * Returns the player cache data.
     *
     * @return all player auths inside the player cache
     */
    public Map<String, PlayerAuth> getCache() {
        return this.cache;
    }

}
