package fr.xephi.authme.cache.auth;

import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class PlayerCache {

    private volatile static PlayerCache singleton;
    private ConcurrentHashMap<String, PlayerAuth> cache;

    private PlayerCache() {
        cache = new ConcurrentHashMap<>();
    }

    /**
     * Method addPlayer.
     * @param auth PlayerAuth
     */
    public void addPlayer(PlayerAuth auth) {
        cache.put(auth.getNickname().toLowerCase(), auth);
    }

    /**
     * Method updatePlayer.
     * @param auth PlayerAuth
     */
    public void updatePlayer(PlayerAuth auth) {
        cache.remove(auth.getNickname().toLowerCase());
        cache.put(auth.getNickname().toLowerCase(), auth);
    }

    /**
     * Method removePlayer.
     * @param user String
     */
    public void removePlayer(String user) {
        cache.remove(user.toLowerCase());
    }

    /**
     * Method isAuthenticated.
     * @param user String
     * @return boolean
     */
    public boolean isAuthenticated(String user) {
        return cache.containsKey(user.toLowerCase());
    }

    /**
     * Method getAuth.
     * @param user String
     * @return PlayerAuth
     */
    public PlayerAuth getAuth(String user) {
        return cache.get(user.toLowerCase());
    }

    /**
     * Method getInstance.
     * @return PlayerCache
     */
    public static PlayerCache getInstance() {
        if (singleton == null) {
            singleton = new PlayerCache();
        }
        return singleton;
    }

    /**
     * Method getLogged.
     * @return int
     */
    public int getLogged() {
        return cache.size();
    }

    /**
     * Method getCache.
     * @return ConcurrentHashMap<String,PlayerAuth>
     */
    public ConcurrentHashMap<String, PlayerAuth> getCache() {
        return this.cache;
    }

}
