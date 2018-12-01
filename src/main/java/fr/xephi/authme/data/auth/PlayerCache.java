package fr.xephi.authme.data.auth;


import fr.xephi.authme.data.player.NamedIdentifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used to manage player's Authenticated status
 */
public class PlayerCache {

    private final Map<NamedIdentifier, PlayerAuth> cache = new ConcurrentHashMap<>();

    PlayerCache() {
    }

    /**
     * Adds the given auth object to the player cache (for the name defined in the PlayerAuth).
     *
     * @param auth the player auth object to save
     */
    public void updatePlayer(PlayerAuth auth) {
        cache.put(auth.toIdentifier(), auth);
    }

    /**
     * Removes a player from the player cache.
     *
     * @param identifier identifier of the player to remove
     */
    public void removePlayer(NamedIdentifier identifier) {
        cache.remove(identifier);
    }

    /**
     * Get whether a player is authenticated (i.e. whether he is present in the player cache).
     *
     * @param identifier player's identifier
     *
     * @return true if player is logged in, false otherwise.
     */
    public boolean isAuthenticated(NamedIdentifier identifier) {
        return cache.containsKey(identifier);
    }

    /**
     * Returns the PlayerAuth associated with the given user, if available.
     *
     * @param identifier identifier of the player
     *
     * @return the associated auth object, or null if not available
     */
    public PlayerAuth getAuth(NamedIdentifier identifier) {
        return cache.get(identifier);
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
    public Map<NamedIdentifier, PlayerAuth> getCache() {
        return this.cache;
    }

}
