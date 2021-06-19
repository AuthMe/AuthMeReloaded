package fr.xephi.authme.data.auth;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used to manage player's Authenticated status
 */
public class PlayerCache {

    private final Map<String, PlayerAuth> cache = new ConcurrentHashMap<>();
    private final Map<String, RegistrationStatus> registeredCache = new ConcurrentHashMap<>();

    PlayerCache() {
    }

    /**
     * Adds the given auth object to the player cache (for the name defined in the PlayerAuth).
     *
     * @param auth the player auth object to save
     */
    public void updatePlayer(PlayerAuth auth) {
        registeredCache.put(auth.getNickname().toLowerCase(), RegistrationStatus.REGISTERED);
        cache.put(auth.getNickname().toLowerCase(), auth);
    }

    /**
     * Removes a player from the player cache.
     *
     * @param user name of the player to remove
     */
    public void removePlayer(String user) {
        cache.remove(user.toLowerCase());
        registeredCache.remove(user.toLowerCase());
    }

    /**
     * Get whether a player is authenticated (i.e. whether he is present in the player cache).
     *
     * @param user player's name
     *
     * @return true if player is logged in, false otherwise.
     */
    public boolean isAuthenticated(String user) {
        return cache.containsKey(user.toLowerCase());
    }

    /**
     * Add a registration entry to the cache for active use later like the player active playing.
     *
     * @param user player name
     * @param status registration status
     */
    public void addRegistrationStatus(String user, RegistrationStatus status) {
        registeredCache.put(user.toLowerCase(), status);
    }

    /**
     * Update the status for existing entries like currently active users
     * @param user player name
     * @param status newest query result
     */
    public void updateRegistrationStatus(String user, RegistrationStatus status) {
        registeredCache.replace(user, status);
    }

    /**
     * Checks if there is cached result with the player having an account.
     * <b>Warning: This shouldn't be used for authentication, because the result could be outdated.</b>
     * @param user player name
     * @return Cached result about being registered or unregistered and UNKNOWN if there is no cache entry
     */
    public RegistrationStatus getRegistrationStatus(String user) {
        return registeredCache.getOrDefault(user.toLowerCase(), RegistrationStatus.UNKNOWN);
    }

    /**
     * Returns the PlayerAuth associated with the given user, if available.
     *
     * @param user name of the player
     *
     * @return the associated auth object, or null if not available
     */
    public PlayerAuth getAuth(String user) {
        return cache.get(user.toLowerCase());
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
    public Map<String, PlayerAuth> getAuthCache() {
        return this.cache;
    }

    public enum RegistrationStatus {
        REGISTERED,
        UNREGISTERED,
        UNKNOWN
    }
}
