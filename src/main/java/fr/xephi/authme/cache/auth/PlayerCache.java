package fr.xephi.authme.cache.auth;

import java.util.HashMap;

public class PlayerCache {

    private static PlayerCache singleton = null;
    private HashMap<String, PlayerAuth> cache;

    private PlayerCache() {
        cache = new HashMap<String, PlayerAuth>();
    }

    public void addPlayer(PlayerAuth auth) {
        cache.put(auth.getNickname(), auth);
    }

    public void updatePlayer(PlayerAuth auth) {
        cache.remove(auth.getNickname());
        cache.put(auth.getNickname(), auth);
    }

    public void removePlayer(String user) {
        cache.remove(user);
    }

    public boolean isAuthenticated(String user) {
        return cache.containsKey(user);
    }

    public PlayerAuth getAuth(String user) {
        return cache.get(user);
    }

    public static PlayerCache getInstance() {
        if (singleton == null) {
            singleton = new PlayerCache();
        }
        return singleton;
    }

    public int getLogged() {
        return cache.size();
    }

}
